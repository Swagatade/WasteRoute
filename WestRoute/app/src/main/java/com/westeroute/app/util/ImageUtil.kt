package com.westeroute.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Photos are stored inside the Firestore complaint document as compressed JPEG data URIs
 * (no Firebase Storage needed). The supervisor web app can render them directly in an <img>.
 */
object ImageUtil {
    private const val PREFIX = "data:image/jpeg;base64,"

    fun toDataUri(bitmap: Bitmap, maxDim: Int = 960, quality: Int = 70): String {
        val longest = maxOf(bitmap.width, bitmap.height).toFloat()
        val scale = minOf(1f, maxDim / longest)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return PREFIX + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Returns null for blank values and for remote http(s) URLs (only embedded photos are decoded). */
    fun fromDataUri(uri: String?): Bitmap? {
        if (uri.isNullOrBlank() || !uri.startsWith("data:image")) return null
        return runCatching {
            val bytes = Base64.decode(uri.substringAfter("base64,"), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
