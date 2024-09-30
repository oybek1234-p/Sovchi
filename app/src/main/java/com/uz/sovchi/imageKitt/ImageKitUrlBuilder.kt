package com.uz.sovchi.imageKitt

object ImageKitUrlBuilder {

    private var stringBuilder = StringBuilder()
    const val endPoint = "https://ik.imagekit.io/startup/${ImageKitUtils.folder}/"

    private const val tr = "?tr="
    private const val quality = "q-"
    private const val width = "w-"
    private const val height = "h-"
    private const val pr = "pr-"
    private const val blur = "bl-"
    private const val auto = "auto"
    private const val format = "f-"
    private const val focus = "fo-"
    private const val coma = ","
    private const val dpr = "dpr-"
    private var first = true

    const val MP4 = ".mp4"
    const val JPEG = "jpeg"
    const val WEBP = "webp"
    const val AVIF = "avif"

    fun newUrl(url: String): ImageKitUrlBuilder {
        stringBuilder.clear()
        stringBuilder.append(endPoint).append(url).append(tr)
        first = true
        return this
    }

    private fun StringBuilder.appendComa(): StringBuilder {
        if (!first) {
            stringBuilder.append(coma)
        }
        return stringBuilder
    }

    fun dpr(value: Float): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(dpr).append(value)
        return this
    }

    fun quality(q: Int): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(quality).append(q)
        first = false
        return ImageKitUrlBuilder
    }

    fun width(w: Int): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(width).append(w.toString())
        first = false
        return this
    }

    fun height(h: Int): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(height).append(h.toString())
        first = false
        return this
    }

    fun progressive(b: Boolean): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(pr).append(if (b) "true" else "false")
        first = false
        return this
    }

    fun blur(b: Int): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(blur).append(b.toString())
        first = false
        return this
    }

    fun format(f: String): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(format).append(f)
        first = false
        return this
    }

    fun autoFocus(): ImageKitUrlBuilder {
        stringBuilder.appendComa().append(focus).append(auto)
        first = false
        return this
    }

    fun get() = stringBuilder.toString()
}