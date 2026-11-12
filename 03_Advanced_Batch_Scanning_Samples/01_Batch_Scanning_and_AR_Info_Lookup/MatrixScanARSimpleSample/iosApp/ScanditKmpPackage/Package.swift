// swift-tools-version:5.9
// This file is part of the Scandit Data Capture SDK.
// Copyright (C) 2026- Scandit AG. All rights reserved.
//
// Local Swift Package consumed by the sample's iosApp target. It bundles:
//   * the sample's Kotlin `shared` umbrella xcframework (built by
//     `:shared:assembleSharedReleaseXCFramework`), and
//   * the Scandit native frameworks it links against, resolved from the
//     public datacapture-spm Swift package declared as a dependency below.
//
// Add this package to the Xcode project as a local dependency:
//   File > Add Package Dependencies > Add Local > select ScanditKmpPackage

import PackageDescription

let package = Package(
    name: "ScanditKmpPackage",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "ScanditKmp",
            targets: [
                "shared", "ScanditKmpNativeDependencies"
            ]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/Scandit/datacapture-spm", exact: "8.7.1")
    ],
    targets: [
        .target(
            name: "ScanditKmpNativeDependencies",
            dependencies: [
                .product(name: "ScanditCaptureCore", package: "datacapture-spm"),
                .product(name: "ScanditBarcodeCapture", package: "datacapture-spm"),
            ]
        ),
        .binaryTarget(
            name: "shared",
            path: "../../shared/build/XCFrameworks/release/shared.xcframework"
        )
    ]
)
