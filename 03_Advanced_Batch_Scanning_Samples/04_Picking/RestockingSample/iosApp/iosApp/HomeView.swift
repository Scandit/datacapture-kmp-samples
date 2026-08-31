/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import AVFoundation
import SwiftUI

// Landing page: Scandit wordmark + title + subtitle + "Start new scan"
// button. Drives the camera-permission flow before navigating into the pick
// screen. Mirrors HomeScreen on Android and the other KMP advanced-batch
// samples.
struct HomeView: View {
    let onStartScan: () -> Void
    @State private var permissionDenied = false

    var body: some View {
        VStack(spacing: 60) {
            VStack(alignment: .leading, spacing: 24) {
                ScanditLogo()
                    .fill(SwiftUI.Color.scanditBlack)
                    .frame(width: 96, height: 14)
                Text("Restocking")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(SwiftUI.Color.scanditTeal)
                Text("Pick and restock products from a list using MatrixScan Pick")
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
