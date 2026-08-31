/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import com.kmp.datacapture.barcode.data.Barcode
import com.kmp.datacapture.core.data.FrameData
import com.kmp.datacapture.core.data.toUIImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation

// iOS counterpart to BarcodeCropper.android.kt. Uses the shared barcodeCropRect
// math; the crop is performed on the frame's CGImage via
// CGImageCreateWithImageInRect. Output orientation is a fixed portrait (.right)
// — a sample-level simplification mirroring the Android actual's fixed 90°.
@OptIn(ExperimentalForeignApi::class)
actual fun cropBarcode(barcode: Barcode, frameData: FrameData): CroppedImage? {
    val sourceImage: UIImage = frameData.imageBuffer?.toUIImage() ?: return null
    val cgImage = sourceImage.CGImage ?: return null

    val frameWidth = CGImageGetWidth(cgImage).toInt()
    val frameHeight = CGImageGetHeight(cgImage).toInt()

    // Safety check when the input image is too small.
    if (frameWidth <= 1 && frameHeight <= 1) return CroppedImage(sourceImage)

    val rect = barcodeCropRect(barcode, frameWidth, frameHeight) ?: return CroppedImage(sourceImage)

    val cropRect = CGRectMake(
        rect.x.toDouble(),
        rect.y.toDouble(),
        rect.width.toDouble(),
        rect.height.toDouble(),
    )
    val croppedCg = CGImageCreateWithImageInRect(cgImage, cropRect) ?: return CroppedImage(sourceImage)

    // .right ≈ portrait-normal, matching the Android actual's fixed 90° rotation.
    val cropped = UIImage(
        cGImage = croppedCg,
        scale = sourceImage.scale,
        orientation = UIImageOrientation.UIImageOrientationRight,
    )
    return CroppedImage(cropped)
}
