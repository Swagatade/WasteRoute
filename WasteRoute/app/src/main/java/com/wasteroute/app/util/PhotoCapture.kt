package com.wasteroute.app.util

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Takes a FULL-RESOLUTION camera photo (not the tiny thumbnail of TakePicturePreview), downsizes it to
 * [maxDim] px, fixes its rotation and hands the bitmap to [onPhoto]. Returns the function that opens the camera
 * (asks for the camera permission first when needed).
 */
@Composable
fun rememberPhotoCapture(
    maxDim: Int = 1280,
    onCancelled: () -> Unit = {},
    onPhoto: (Bitmap) -> Unit
): () -> Unit {
    val ctx = LocalContext.current
    val onPhotoNow by rememberUpdatedState(onPhoto)
    val onCancelledNow by rememberUpdatedState(onCancelled)
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPath
        pendingPath = null
        val bitmap = if (ok && path != null) decodeScaled(path, maxDim) else null
        path?.let { runCatching { File(it).delete() } }
        if (bitmap != null) onPhotoNow(bitmap) else onCancelledNow()
    }

    val launchCamera = {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        pendingPath = file.absolutePath
        takePicture.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file))
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else onCancelledNow()
    }

    return {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }
}

private fun decodeScaled(path: String, maxDim: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@runCatching null
    val rotation = when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) bmp else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
}.getOrNull()
