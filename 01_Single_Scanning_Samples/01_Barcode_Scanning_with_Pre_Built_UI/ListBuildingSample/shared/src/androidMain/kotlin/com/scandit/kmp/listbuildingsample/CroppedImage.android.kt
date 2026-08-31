/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

import android.graphics.Bitmap

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class CroppedImage internal constructor(internal val bitmap: Bitmap)

/** The underlying [Bitmap] for the Compose `Image` to render. */
fun CroppedImage.toBitmap(): Bitmap = bitmap
