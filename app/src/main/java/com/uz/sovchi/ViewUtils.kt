package com.uz.sovchi

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.postDelayed
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputLayout
import com.uz.sovchi.imageKitt.ImageKitUtils
import jp.wasabeef.glide.transformations.BlurTransformation
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

fun ImageView.loadPhoto(string: String?, blur: Boolean = false) {
    if (string.isNullOrEmpty()) {
        setImageDrawable(null)
        return
    }
    val isImageKit = ImageKitUtils.isUrlImageKit(string)
    var loadUrl: Any = if (isImageKit) {
        ImageKitUtils.buildUrl(
            string, false, blur = if (blur) 25 else 0
        )
    } else string
    var notBlur = false
    when (string) {
        "kelin" -> {
            loadUrl = R.drawable.woman_placeholder
            notBlur = true
        }

        "kuyov" -> {
            loadUrl = R.drawable.man_placeholder
            notBlur = true
        }
    }
    if (isImageKit) {
        Glide.with(appContext).load(loadUrl)
            .transition(DrawableTransitionOptions.withCrossFade(150)).into(this)
    } else {
        var req = Glide.with(appContext).load(loadUrl)
        if (blur && notBlur.not()) {
            req = req.transform(BlurTransformation(70))
        }
        req.transition(DrawableTransitionOptions.withCrossFade(150)).into(this)
    }
}

fun TextInputLayout.setEditTextErrorIf(errorText: String, condition: () -> Boolean) {
    editText?.setOnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) {
            if (condition.invoke()) {
                error = errorText
                isErrorEnabled = true
            }
        } else {
            error = null
            isErrorEnabled = false
        }
    }
}

fun Context.getMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is android.content.ContextWrapper -> baseContext.getMainActivity()
        else -> null
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
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = activity.currentFocus
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    } catch (e: Exception) {
        handleException(e)
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

fun Context.getMaterialColor(colorId: Int) = MaterialColors.getColor(
    this, colorId, 0
)

fun <T> MutableLiveData<T>.update() {
    postVal(value)
}

fun <T> MutableLiveData<T>.postVal(value: T?) {
    MainScope().launch {
        try {
            this@postVal.value = value
        } catch (e: Exception) {
            handleException(e)
        }
    }
}

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
