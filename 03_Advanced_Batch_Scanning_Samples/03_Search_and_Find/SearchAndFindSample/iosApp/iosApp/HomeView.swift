/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import AVFoundation
import SwiftUI

// Landing page: Scandit wordmark + title + subtitle + "Start new scan"
// button. Drives the camera-permission flow before navigating into the
// scanner screen.
struct HomeView: View {
    let onStartScan: () -> Void
    @State private var permissionDenied = false

    var body: some View {
        VStack(spacing: 60) {
            VStack(alignment: .leading, spacing: 24) {
                ScanditLogo()
                    .fill(SwiftUI.Color.scanditBlack)
                    .frame(width: 96, height: 14)
                Text("Search & Find")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(SwiftUI.Color.scanditCyan)
                    .textCase(nil)
                Text("Search for and find barcodes with SparkScan and MatrixScan Find")
                    .font(.system(size: 16))
                    .foregroundColor(SwiftUI.Color.scanditBlack)
            }
            .frame(maxWidth: 350, alignment: .leading)

            Button(action: handleStart) {
                HStack(spacing: 8) {
                    ScanIconShape()
                        .fill(SwiftUI.Color.white)
                        .frame(width: 20, height: 20)
                    Text("Start new scan")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.white)
                }
                .frame(maxWidth: 350)
                .padding(.vertical, 12)
                .padding(.horizontal, 32)
                .background(SwiftUI.Color.scanditBlack)
                .cornerRadius(8)
            }

            if permissionDenied {
                Text("Camera permission is required to scan barcodes.")
                    .foregroundColor(SwiftUI.Color.scanditBlack)
                    .padding(.horizontal)
            }
        }
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(SwiftUI.Color.white)
    }

    private func handleStart() {
        permissionDenied = false
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            onStartScan()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    if granted { onStartScan() } else { permissionDenied = true }
                }
            }
        default:
            permissionDenied = true
        }
    }
}
