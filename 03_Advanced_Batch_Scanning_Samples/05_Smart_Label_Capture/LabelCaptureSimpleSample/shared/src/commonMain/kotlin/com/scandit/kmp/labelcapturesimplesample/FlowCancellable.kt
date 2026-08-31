/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.labelcapturesimplesample

import kotlinx.coroutines.Job

// Swift-visible handle returned by [LabelScannerScreenModel.observeState].
// SwiftUI hosts hold one of these for the lifetime of the view and call
// [cancel] in `deinit` to stop the StateFlow collector. The wrapper exists
// because raw `kotlinx.coroutines.Job` is hard to use from ObjC/Swift
// (Job has many erased generics + cancellation semantics that don't map
// cleanly across the bridge).
class FlowCancellable internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}
