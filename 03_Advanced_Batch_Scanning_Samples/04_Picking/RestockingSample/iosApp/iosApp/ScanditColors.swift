/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import SwiftUI

// Palette lifted directly from the native RestockingSample's colors.xml so
// the result screen (Picklist / Inventory headers, picked checkmark) matches
// look-for-look. Named SwiftUI colors are the lightweight equivalent of a
// Compose `ScanditTheme` on Android — views reference the brand by name
// (`Color.scanditBlack`) instead of hard-coding hex literals at the call
// site.
//
// Internal (not private) so every other view file in this target can use
// `Color.scanditBlack` etc. — the same way the Android `ScanditColors`
// object is internal to the sample module.
extension Color {
    static let scanditBlack = Color(hex: 0x000000)
    static let scanditWhite = Color(hex: 0xFFFFFF)
    static let scanditLightGrey = Color(hex: 0xF8FAFC)
    static let scanditBlack900 = Color(hex: 0x121619)
    static let scanditGrey500 = Color(hex: 0x8795A1)
    static let scanditTeal = Color(hex: 0x2EC1CE)
    static let scanditRed = Color(hex: 0xFA4446)
    static let scanditAmber = Color(hex: 0xFBC02C)
    static let scanditGreySlimSeparator = Color(hex: 0xDAE1E7)

    init(hex: UInt32, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}
