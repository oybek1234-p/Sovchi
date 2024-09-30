package com.uz.sovchi.imageKitt

import androidx.annotation.Keep

@Keep
object ImageKitUtils {

    const val imageKitIndex = "imageKit"
    const val folder = "nikohImages"
    private const val downscaleValue = 80
    private const val fullQualityValue = 90

    private const val DEFAULT_FORMAT = ImageKitUrlBuilder.WEBP

    fun buildUrl(
        fileName: String,
        fullQuality: Boolean = false,
        blur: Int = 0,
        format: String = DEFAULT_FORMAT,
        width: Int = 0,
        height: Int = 0
    ): String {
        var url = ImageKitUrlBuilder.newUrl(fileName)
            .quality(if (fullQuality) fullQualityValue else downscaleValue).format(format)
            .width(width).height(height)
        if (blur > 0) {
            url = url.blur(blur)
        }
        return url.get()
    }

    const val endPoint = "https://ik.imagekit.io/startup/$folder/"

    fun isUrlImageKit(url: String): Boolean {
        return url.startsWith(imageKitIndex) || url.startsWith(endPoint)
    }

}