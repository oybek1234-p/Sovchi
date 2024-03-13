package com.uz.sovchi

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.postDelayed
import androidx.lifecycle.MutableLiveData
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun View.visibleOrGone(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun showToast(message: String, context: Context = appContext) {
    MainScope().launch {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

private fun showKeyboard(view: View?) {
    view?.postDelayed(0) {
        try {
            if (view is EditText) {
                view.requestFocus()
            }
            val inputManager =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            //
        }
    }
}

fun EditText.showKeyboard() {
    showKeyboard(this)
}

fun hideSoftInput(activity: Activity) {
    try {
        val manager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(
            activity.currentFocus?.windowToken, InputMethodManager.HIDE_IMPLICIT_ONLY
        )
    } catch (e: java.lang.Exception) {
        ExceptionHandler.handle(e)
    }
}

fun toDp(value: Float, appContext: Context): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, appContext.resources.displayMetrics
    ).toInt()
}

fun Context.toDp(value: Int): Int {
    return toDp(value.toFloat(), this)
}

fun Context.getMaterialColor(colorId: Int) =
    MaterialColors.getColor(
        this,
        colorId,
        0
    )

fun <T> MutableLiveData<T>.update() = postValue(value)

fun TextView.makeExpandable() {
    val currentLine = maxLines
    var expanded = false
    setOnClickListener {
        maxLines = if (expanded) {
            currentLine
        } else {
            Int.MAX_VALUE
        }
        expanded = !expanded
    }
}
