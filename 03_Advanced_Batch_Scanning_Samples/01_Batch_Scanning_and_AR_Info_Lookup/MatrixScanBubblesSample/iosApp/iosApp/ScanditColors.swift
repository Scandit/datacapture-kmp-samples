/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI

// Scandit brand palette (see Scandit_Brand_Colors_2025).
//
// Named SwiftUI colors are the lightweight equivalent of a Compose
// `ScanditTheme` on Android — views reference the brand by name
// (`Color.scanditBlack`) instead of hard-coding hex literals at the
// call site.
//
// Internal (not private) so every other view file in this target can use
// `Color.scanditBlack` etc. — the same way the Android `ScanditColors`
// object is internal to the sample module.
extension Color {
    static let scanditBlack = Color(hex: 0x141F2E)
    static let scanditWhite = Color(hex: 0xFFFFFF)
    static let scanditCyan = Color(hex: 0x2EC1CE)
    static let scanditLightGrey = Color(hex: 0xF1F5F8)
    static let scanditBlue = Color(hex: 0x0252B5)

    init(hex: UInt32, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}
