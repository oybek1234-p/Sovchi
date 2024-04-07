package com.uz.sovchi.data.filter

import android.content.Context
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uz.sovchi.AutoCompleteView
import com.uz.sovchi.appContext
import com.uz.sovchi.data.location.City
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.data.nomzod.OilaviyHolati
import com.uz.sovchi.data.nomzod.getNomzodTypeText
import com.uz.sovchi.data.nomzod.nomzodTypes

@Entity
data class SavedFilterUser(
    @PrimaryKey var userId: String, var manzil: String, var nomzodType: Int,
    var oilaviyHolati: String, var yoshChegarasi: Int, var imkonChek: Boolean,var hasPhoto: Boolean
) {
    constructor() : this("", City.Hammasi.name, KELIN, OilaviyHolati.Aralash.name, 0, false,false)
}

object MyFilter {

    private var savedFilterPrefs =
        appContext.getSharedPreferences("SavedFilter", Context.MODE_PRIVATE)

    var filter = SavedFilterUser()

    fun changedFromDefault(): Boolean {
        filter.apply {
            return manzil != City.Hammasi.name || OilaviyHolati.Aralash.name != oilaviyHolati || nomzodType != KELIN || yoshChegarasi != 0 || imkonChek
        }
    }

    fun get() {
        savedFilterPrefs.apply {
            filter.nomzodType = getInt("nTyp", KELIN)
            filter.manzil = getString("manzil", City.Hammasi.name)!!
            filter.oilaviyHolati = getString("oilHolati", OilaviyHolati.Aralash.name)!!
            filter.yoshChegarasi = getInt("yoshCheg", 0)
            filter.imkonChek = getBoolean("imChek", false)
            filter.hasPhoto = getBoolean("hPhoto",false)
        }
    }

    fun update() {
        savedFilterPrefs.edit().apply {
            putInt("nTyp", filter.nomzodType)
            putString("manzil", filter.manzil)
            putString("oilHolati", filter.oilaviyHolati)
            putInt("yoshCheg", filter.yoshChegarasi)
            putBoolean("imChek", filter.imkonChek)
            putBoolean("hPhoto", filter.hasPhoto)
            apply()
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

    fun setYoshChegarasiView(editText: EditText, update: () -> Unit) {
        val currentNumber = MyFilter.filter.yoshChegarasi
        editText.setText(if (currentNumber == 0) "" else currentNumber.toString())
        editText.addTextChangedListener {
            val number = it?.toString()?.toIntOrNull() ?: 0
            updateFilter {
                yoshChegarasi = if (number > 17) {
                    number
                } else {
                    0
                }
                update.invoke()
            }
        }
    }

    fun setImkoniyatiCheklangan(checkBox: CheckBox, update: () -> Unit) {
        checkBox.isChecked = MyFilter.filter.imkonChek
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            updateFilter {
                imkonChek = isChecked
                update.invoke()
            }
        }
    }

    fun setOilaviyHolati(autoCompleteTextView: AutoCompleteTextView, update: () -> Unit) {
        autoCompleteTextView.apply {
            val types = OilaviyHolati.entries.map { context.getString(it.resourceId) }
            AutoCompleteView.setUpAutoCompleteView(
                this,
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
            AutoCompleteView.setUpAutoCompleteView(
                this,
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