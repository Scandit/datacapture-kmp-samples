/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import android.graphics.Bitmap
import android.graphics.Matrix
import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.data.FrameData
import com.kmp.datacapture.core.data.toBitmap
import kotlin.math.min

// Crops the live frame around the barcode → scales so the smaller side is
// ~100px → rotates to portrait. The KMP frame pipeline doesn't expose the
// host window rotation in commonMain, so a fixed 90° portrait rotation is
// applied (a sample-level simplification).
actual fun cropBarcode(barcode: Barcode, frameData: FrameData): CroppedImage? {
    val frame: Bitmap = frameData.imageBuffer?.toBitmap() ?: return null

    // Safety check when the input bitmap is too small.
    if (frame.width == 1 && frame.height == 1) return CroppedImage(frame)

    val rect = barcodeCropRect(barcode, frame.width, frame.height) ?: return CroppedImage(frame)

    val scale = SCALED_IMAGE_SIZE_IN_PIXELS / min(rect.width, rect.height).toFloat()
    val matrix = Matrix().apply {
        postRotate(PORTRAIT_ROTATION_DEGREES)
        postScale(scale, scale)
    }

    return CroppedImage(
        Bitmap.createBitmap(frame, rect.x, rect.y, rect.width, rect.height, matrix, true),
    )
}

// Portrait-normal rotation, matching the native sample's ROTATION_0 case.
private const val PORTRAIT_ROTATION_DEGREES = 90f
