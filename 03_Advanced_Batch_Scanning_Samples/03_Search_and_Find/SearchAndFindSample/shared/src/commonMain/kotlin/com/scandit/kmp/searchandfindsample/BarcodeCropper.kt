/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.data.FrameData
import kotlin.math.max
import kotlin.math.min

/** Padding applied around the barcode bounding box before cropping. */
internal const val CROPPED_IMAGE_PADDING = 1.2f

/** Target size (px) of the smaller dimension of the scaled thumbnail. */
internal const val SCALED_IMAGE_SIZE_IN_PIXELS = 100

/** A clamped, frame-relative crop rectangle in pixels. */
internal data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * Pure crop-rect math shared by both platform actuals (kept here so it's
 * unit-testable): bounding box of [barcode]'s four corners → square (longest
 * side) × [CROPPED_IMAGE_PADDING] padding, centred and clamped to the frame.
 * Returns `null` when the result is empty.
 */
internal fun barcodeCropRect(barcode: Barcode, frameWidth: Int, frameHeight: Int): CropRect? {
    val location = barcode.location
    val corners = listOf(
        location.bottomLeft,
        location.topLeft,
        location.topRight,
        location.bottomRight,
    )

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE
    for (p in corners) {
        minX = min(minX, p.x)
        maxX = max(maxX, p.x)
        minY = min(minY, p.y)
        maxY = max(maxY, p.y)
    }

    val size = max(maxY - minY, maxX - minX) * CROPPED_IMAGE_PADDING
    val x = max(((minX + maxX - size) / 2f).toInt(), 0)
    val y = max(((minY + maxY - size) / 2f).toInt(), 0)

    // Keep the crop square and fully inside the frame: the largest side that
    // fits from (x, y) in both axes. Clamping each axis independently isn't
    // enough — the tighter bound must win on both, or Bitmap.createBitmap /
    // CGImageCreateWithImageInRect throws on the out-of-bounds rect.
    val side = minOf(size.toInt(), frameWidth - x, frameHeight - y)

    if (side <= 0) return null
    return CropRect(x, y, side, side)
}

/**
 * Crops the region of the frame around [barcode] and scales it down to a small
 * thumbnail: bounding box of the four corners → square (longest side) × 1.2
 * padding, clamped to frame bounds → scaled so the smaller side is ~100px →
 * rotated to portrait.
 *
 * Returns `null` when no image is attached to the frame or the computed crop is
 * empty. The platform actual reads the native image from
 * [FrameData.imageBuffer] (`toBitmap()` / `toUIImage()`).
 */
expect fun cropBarcode(barcode: Barcode, frameData: FrameData): CroppedImage?
