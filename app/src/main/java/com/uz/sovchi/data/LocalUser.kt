package com.uz.sovchi.data

import android.content.Context
import android.content.SharedPreferences
import com.uz.sovchi.appContext

object LocalUser {

    var user = User()

    private fun preference(context: Context): SharedPreferences =
        (context.getSharedPreferences("localUser", Context.MODE_PRIVATE))

    fun getUser(context: Context) {
        val pref = preference(appContext)
        with(pref) {
            user.apply {
                uid = getString("uid", "") ?: ""
                name = getString("name", "") ?: ""
                phoneNumber = getString("phone", "") ?: ""
                lastSeenTime = getLong("lastSeenTime", 0L)
                hasNomzod = getBoolean("hNomzod",false)
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
                commit()
            }
        }
    }
}