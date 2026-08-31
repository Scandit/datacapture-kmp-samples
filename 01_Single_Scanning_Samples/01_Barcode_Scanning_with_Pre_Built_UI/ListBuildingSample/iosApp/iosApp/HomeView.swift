/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import AVFoundation
import SwiftUI

// Landing page for the ListBuildingSample: Scandit wordmark + title +
// subtitle + "Start new scan" button. Drives the camera-permission flow
// before navigating into the list-building screen.
struct HomeView: View {
    let onStartScan: () -> Void
    @State private var permissionDenied = false

    var body: some View {
        VStack(spacing: 60) {
            VStack(alignment: .leading, spacing: 24) {
                ScanditLogo()
                    .fill(Color.scanditBlack)
                    .frame(width: 96, height: 14)
                Text("List Building")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(Color.scanditCyan)
                Text("Build a list of scanned barcodes using the SparkScan pre-built UI")
                    .font(.system(size: 16))
                    .foregroundColor(Color.scanditBlack)
            }
            .frame(maxWidth: 350, alignment: .leading)

            Button(action: handleStart) {
                HStack(spacing: 8) {
                    ScanIconShape()
                        .fill(Color.white)
                        .frame(width: 20, height: 20)
                    Text("Start new scan")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.white)
                }
                .frame(maxWidth: 350)
                .padding(.vertical, 12)
                .padding(.horizontal, 32)
                .background(Color.scanditBlack)
                .cornerRadius(8)
            }

            if permissionDenied {
                Text("Camera permission is required to scan barcodes.")
                    .foregroundColor(Color.scanditBlack)
                    .padding(.horizontal)
            }
        }
        .padding(.horizontal, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.white)
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
