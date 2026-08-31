/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.searchandfindsample

import platform.UIKit.UIImage

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class CroppedImage internal constructor(internal val image: UIImage)

/** The underlying [UIImage] for SwiftUI to render. */
fun CroppedImage.toUIImage(): UIImage = image
