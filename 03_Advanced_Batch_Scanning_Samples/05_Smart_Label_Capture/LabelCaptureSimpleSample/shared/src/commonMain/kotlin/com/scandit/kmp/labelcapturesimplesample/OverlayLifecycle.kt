/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.labelcapturesimplesample

// Who owns the camera across the host's resume/pause lifecycle.
//
// On Android the validation-flow overlay drives the camera itself:
// forwarding onResume()/onPause() is all the host must do (mirrors the
// native LabelCaptureSimpleSample). The overlay restores its own state on
// resume — running stays running, and a pause (via the overlay's Pause
// button or after a validated capture) stays paused. Forcing the camera ON
// here is what clobbered the paused state when the app was restored from
// the background (SDC-32631).
//
// On iOS the overlay's lifecycle hooks are no-ops and STARTED/STOPPED only
// fire on view (dis)appearance, so the host switches the camera itself.
internal expect val overlayDrivesCameraLifecycle: Boolean
