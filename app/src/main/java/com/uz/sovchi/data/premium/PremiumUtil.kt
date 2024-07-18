package com.uz.sovchi.data.premium

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.uz.sovchi.DateUtils
import com.uz.sovchi.appContext
import com.uz.sovchi.showToast

data class ViewLimit(var viewed: Int, var date: Long) {
    fun isExpired(): Boolean {
        val isSameDay = DateUtils.isToday(date)
        return isSameDay && viewed >= 10
    }
}

object PremiumUtil {

    private val sharedPrefs = appContext.getSharedPreferences("premiumLimit", Context.MODE_PRIVATE)

    var viewLimit = ViewLimit(0, 0)

    const val PREMIUM_DEFAULT_PRICE = 35000L

    private val premiumReference = FirebaseDatabase.getInstance().getReference("premiumPrice")

    fun loadPremiumPrice(done: (premiumPrice: Long) -> Unit) {
        premiumReference.get().addOnSuccessListener {
            val price = it.value.toString().toLongOrNull() ?: PREMIUM_DEFAULT_PRICE
            done.invoke(price)
        }.addOnFailureListener {
            done.invoke(PREMIUM_DEFAULT_PRICE)
        }
    }

    fun get() {
        sharedPrefs.apply {
            viewLimit = ViewLimit(getInt("viewed", 0), getLong("date", 0))
            if (DateUtils.isToday(viewLimit.date).not()) {
                viewLimit.viewed = 0
                viewLimit.date = System.currentTimeMillis()
                edit().clear().apply()
            }
        }
    }

    fun setViewedOneMore() {
        viewLimit.viewed += 1
        viewLimit.date = System.currentTimeMillis()
        sharedPrefs.edit().putInt("viewed", viewLimit.viewed).putLong("date", viewLimit.date)
            .apply()
    }
}