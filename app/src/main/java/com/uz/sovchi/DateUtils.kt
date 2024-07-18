package com.uz.sovchi

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    fun formatDate(date: Long): String {
        if (date == 0L) return appContext.getString(R.string.yaqinda)
        return Date(date).getTimeAgo()
    }

    private const val DAY_FORMAT_YEAR = "yyyy dd MMMM"
    private const val DAY_FORMAT = "dd MMMM"
    private const val FULL_DATE_FORMAT = "hh:mm dd-MM-yyyy"
    private const val SMALL_DATE_FORMAT = "dd-MM-yyyy"
    private const val ONLY_TIME_FORMAT = "hh:mm"
    private const val TIME_AND_DAY = "hh:mm dd MMMM"

    private val calendar = Calendar.getInstance()
    private val date = Date()

    @SuppressLint("ConstantLocale", "NewApi")
    private val timeFormatString =
        android.icu.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss aaa", Locale.getDefault())

    private const val ONLY_DAY = 0
    private const val ONLY_TIME = 1
    const val FULL_DATE = 2
    private const val SMALL_DATE = 3
    private const val DAY_WITH_YEAR = 4
    private const val TIME_DAY = 5

    fun isSameDay(first: Long, second: Long): Boolean {
        date.time = first
        calendar.time = date
        val firstDay = calendar.get(Calendar.DAY_OF_YEAR)
        val firstYear = calendar.get(Calendar.YEAR)

        date.time = second
        calendar.time = date
        val secondDay = calendar.get(Calendar.DAY_OF_YEAR)
        val secondYear = calendar.get(Calendar.YEAR)

        return firstYear == secondYear && secondDay == firstDay
    }

    fun isSameYear(first: Long, second: Long): Boolean {
        date.time = first
        calendar.time = date
        val firstYear = calendar.get(Calendar.YEAR)

        date.time = second
        calendar.time = date
        val secondYear = calendar.get(Calendar.YEAR)

        return firstYear == secondYear
    }

    fun isToday(date: Long): Boolean {
        return isSameDay(date, System.currentTimeMillis())
    }

    fun isYesterday(d: Long): Boolean {
        return isToday(d + android.text.format.DateUtils.DAY_IN_MILLIS)
    }

    fun getHourMinuteDayMonth(date: Long): String {
        val time = date
        val outputFmt = SimpleDateFormat("HH:mm   dd.MM.yyyy")
        return outputFmt.format(time)
    }

    fun utcToCurrent(utc: Long): Long {
        val offset: Int = TimeZone.getDefault().rawOffset + TimeZone.getDefault().dstSavings
        return utc * 1000 - offset
    }

    private fun formatDate(millis: Long, type: Int, year: Boolean = false): String {
        val pattern = when (type) {
            ONLY_DAY -> DAY_FORMAT
            ONLY_TIME -> ONLY_TIME_FORMAT
            FULL_DATE -> FULL_DATE_FORMAT
            SMALL_DATE -> SMALL_DATE_FORMAT
            DAY_WITH_YEAR -> DAY_FORMAT_YEAR
            TIME_DAY -> TIME_AND_DAY
            else -> FULL_DATE_FORMAT
        }
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        return format.format(millis)
    }

    private fun formatDateText(date: Long, type: Int): String {
        if (isToday(date)) {
            return formatDate(date, ONLY_TIME) + " ${appContext.getString(R.string.today)}"
        }
        if (isYesterday(date)) {
            return formatDate(date, ONLY_TIME) + " ${appContext.getString(R.string.today)}"
        }
        return formatDate(date, type)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun formatTimeString(time: String): String {
        val date = timeFormatString.parse(time)
        if (date != null) {
            return formatDateText(date.time, TIME_DAY)
        }
        return ""
    }

    private fun Date.getTimeAgo(): String {
        val calendar = Calendar.getInstance()
        calendar.time = this

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val currentCalendar = Calendar.getInstance()

        val currentYear = currentCalendar.get(Calendar.YEAR)
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentCalendar.get(Calendar.MINUTE)

        return if (year < currentYear) {
            val interval = currentYear - year
            "$interval ${getString(R.string.yil_oldin)}"
        } else if (month < currentMonth) {
            val interval = currentMonth - month
            "$interval ${getString(R.string.oy_oldin)}"
        } else if (day < currentDay) {
            val interval = currentDay - day
            "$interval ${getString(R.string.kun_oldin)}"
        } else if (hour < currentHour) {
            val interval = currentHour - hour
            "$interval ${getString(R.string.sohat_oldin)}"
        } else if (minute < currentMinute) {
            val interval = currentMinute - minute
            "$interval ${getString(R.string.daqiqa_oldin)}"
        } else {
            getString(R.string.yaqinda)
        }
    }

    private fun getString(id: Int) = appContext.getString(id)
}