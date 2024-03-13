package com.uz.sovchi.data

import android.content.Context
import android.content.SharedPreferences

object LocalUser {

    var user = User()

    private fun preference(context: Context): SharedPreferences =
        (context.getSharedPreferences("localUser", Context.MODE_PRIVATE))

    fun getUser(context: Context) {
        val pref = preference(context)
        with(pref) {
            user.apply {
                uid = getString("uid", "") ?: ""
                name = getString("name", "") ?: ""
                phoneNumber = getString("phone", "") ?: ""
                lastSeenTime = getLong("lastSeenTime", 0L)
            }
        }
    }

    fun saveUser(context: Context) {
        val pref = preference(context).edit()
        with(pref) {
            user.apply {
                putString("uid", uid)
                putString("name", name)
                putString("phone", phoneNumber)
                putLong("lastSeenTime", lastSeenTime)
                commit()
            }
        }
    }
}