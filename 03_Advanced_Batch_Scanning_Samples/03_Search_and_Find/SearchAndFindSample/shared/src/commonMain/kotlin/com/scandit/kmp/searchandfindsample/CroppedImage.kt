/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

/**
 * A cropped, scaled thumbnail of a single scanned barcode.
 *
 * Wraps the platform's natural image type — `android.graphics.Bitmap` on
 * Android, `UIImage` on iOS — so common code can hold and pass thumbnails
 * around (e.g. in [SearchScannedBarcode]) while each platform renders the underlying
 * native image directly. The accessor that exposes the native value lives in
 * the platform actual:
 * - Android: `fun CroppedImage.toBitmap(): Bitmap`
 * - iOS:     `fun CroppedImage.toUIImage(): UIImage`
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class CroppedImage
