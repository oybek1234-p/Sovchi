package com.uz.sovchi.data.filter

import android.content.Context
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.uz.sovchi.AutoCompleteView
import com.uz.sovchi.appContext
import com.uz.sovchi.data.LocalUser
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.getNomzodTypeText
import com.uz.sovchi.data.nomzod.nomzodTypes
import com.uz.sovchi.data.valid
import com.uz.sovchi.update
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

object MyFilter {

    const val AGE_MIN = 18
    const val AGE_MAX = 70

    private var savedFilterPrefs =
        appContext.getSharedPreferences("SavedFilters", Context.MODE_PRIVATE)

    var filter = SavedFilterUser()
    var filterChangeObserver = MutableLiveData<Any>()

    fun changedFromDefault(): Boolean {
        filter.apply {
            return manzil != City.Hammasi.name || OilaviyHolati.Aralash.name != oilaviyHolati || nomzodType != KELIN || yoshChegarasiDan != 0 || yoshChegarasiGacha != 0 || imkonChek
        }
    }

    private val filtersReference = FirebaseFirestore.getInstance().collection("filters")

    init {
        get()
    }

    fun get() {
        savedFilterPrefs.apply {
            filter.nomzodType = getInt("nTyp", KELIN)
            filter.manzil = getString("manzil", City.Hammasi.name)!!
            filter.oilaviyHolati = getString("oilHolati", OilaviyHolati.Aralash.name)!!
            filter.yoshChegarasiDan = getInt(
                "yoshChegDan", AGE_MIN
            ).let { if (it < AGE_MIN || it > AGE_MAX) AGE_MIN else it }
            filter.yoshChegarasiGacha = getInt(
                "yoshChegGacha", AGE_MAX
            ).let { if (it > AGE_MAX || it < AGE_MIN) AGE_MAX else it }
            filter.imkonChek = getBoolean("imChek", false)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun update() {
        GlobalScope.launch(Dispatchers.Default) {
            savedFilterPrefs.edit().apply {
                putInt("nTyp", filter.nomzodType)
                putString("manzil", filter.manzil)
                putString("oilHolati", filter.oilaviyHolati)
                putInt("yoshChegDan", filter.yoshChegarasiDan)
                putInt("yoshChegGacha", filter.yoshChegarasiGacha)
                putBoolean("imChek", filter.imkonChek)
                filter.date = System.currentTimeMillis()
                apply()
                if (LocalUser.user.valid) {
                    filter.userId = LocalUser.user.uid
                    filter.phoneNumber = LocalUser.user.phoneNumber.removePrefix("+")
                    filtersReference.document(filter.userId).set(filter)
                }
                MainScope().launch {
                    filterChangeObserver.update()
                }
            }
        }
    }
}

object FilterViewUtils {

    fun updateFilter(filter: SavedFilterUser.() -> Unit) {
        MyFilter.filter.apply(filter)
        MyFilter.update()
    }

    fun setLocationView(autoCompleteTextView: AutoCompleteTextView, update: () -> Unit) {
        autoCompleteTextView.apply {
            val types = City.asListNames()
            val adapter =
                AutoCompleteView.createAutoCompleteAdapter(autoCompleteTextView.context, types)
            setText(context.getString(City.valueOf(MyFilter.filter.manzil).resId))
            setAdapter(adapter)
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
                updateFilter {
                    manzil = City.entries[position].name
                    update.invoke()
                }
            }
        }
    }

    fun setOilaviyHolati(autoCompleteTextView: AutoCompleteTextView, update: () -> Unit) {
        autoCompleteTextView.apply {
            val types = OilaviyHolati.entries.map { context.getString(it.resourceId) }
            AutoCompleteView.setUpAutoCompleteView(this,
                types,
                context.getString(OilaviyHolati.valueOf(MyFilter.filter.oilaviyHolati).resourceId),
                click = {
                    updateFilter {
                        oilaviyHolati = OilaviyHolati.entries[it].name
                        update.invoke()
                    }
                })

        }
    }

    fun setNomzodTypeView(autoCompleteTextView: AutoCompleteTextView, update: () -> Unit) {
        autoCompleteTextView.apply {
            val types = nomzodTypes.map { it.second }
            AutoCompleteView.setUpAutoCompleteView(this,
                types,
                getNomzodTypeText(MyFilter.filter.nomzodType) ?: "",
                click = {
                    updateFilter {
                        nomzodType = nomzodTypes[it].first
                        update.invoke()
                    }
                }

            )
        }
    }
}