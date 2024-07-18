package com.uz.sovchi.data

import android.content.Context
import android.content.SharedPreferences
import com.uz.sovchi.appContext
import com.uz.sovchi.showToast

object LocalUser {

    var user = User()

    private fun preference(context: Context): SharedPreferences =
        (context.getSharedPreferences("localUser", Context.MODE_PRIVATE))

    fun getUser(context: Context = appContext) {
        val pref = preference(appContext)
        with(pref) {
            user.apply {
                uid = getString("uid", "") ?: ""
                name = getString("name", "") ?: ""
                phoneNumber = getString("phone", "") ?: ""
                lastSeenTime = getLong("lastSeenTime", 0L)
                hasNomzod = getBoolean("hNomzod",false)
                premium = getBoolean("premium",false)
                premiumDate = getLong("premiumDate",0)
            }
        }
    }

    fun saveUser(context: Context = appContext) {
        val pref = preference(appContext).edit()
        with(pref) {
            user.apply {
                putString("uid", uid)
                putString("name", name)
                putString("phone", phoneNumber)
                putLong("lastSeenTime", lastSeenTime)
                putBoolean("hNomzod",hasNomzod)
                putBoolean("premium",premium)
                putLong("premiumDate",premiumDate)
                commit()
            }
        }
    }
}