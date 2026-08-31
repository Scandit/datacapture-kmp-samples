/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Combine
import SwiftUI
import shared

// State-owned holder pinning the shared screen model + its flow observer to
// this view's lifetime. SwiftUI re-runs `ListBuildingHost.init()` on every
// body recomputation, so a plain stored property would build a fresh model
// each time; routing everything through one `@StateObject` keeps the observer
// and the UI actions pointing at the same instance.
final class ListBuildingHostState: ObservableObject {
    let screenModel: ListBuildingScreenModel
    let sparkScanView: SparkScanView
    @Published var items: [ListBuildingEntry] = []
    @Published var scannedCount: Int = 0

    private let observer: FlowObserver
    private var itemsCancellable: AnyCancellable?
    private var countCancellable: AnyCancellable?

    init() {
        let model = ListBuildingScreenModel()
        self.screenModel = model
        // iOS SparkScanView ctor takes no host parameter (SDC-31709) — its
        // internal SDCSparkScanView starts at zero frame and follows Auto
        // Layout once embedded by the UIViewRepresentable below.
        let view = SparkScanView(
            dataCaptureContext: model.dataCaptureContext,
            sparkScan: model.sparkScan,
            settings: model.sparkScanViewSettings
        )
        view.feedbackDelegate = model
        self.sparkScanView = view

        self.observer = FlowObserver(model: model)
        self.items = observer.items
        self.scannedCount = observer.scannedCount
        self.itemsCancellable = observer.$items
            .receive(on: DispatchQueue.main)
            .assign(to: \.items, on: self)
        self.countCancellable = observer.$scannedCount
            .receive(on: DispatchQueue.main)
            .assign(to: \.scannedCount, on: self)
    }

    deinit {
        sparkScanView.feedbackDelegate = nil
        screenModel.dispose()
    }
}

struct ListBuildingHost: View {
    @StateObject private var host = ListBuildingHostState()

    private var countLabel: String {
        host.scannedCount == 1
            ? "\(host.scannedCount) item scanned"
            : "\(host.scannedCount) items scanned"
    }

    var body: some View {
        VStack(spacing: 0) {
            Text(countLabel)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color.scanditBlack)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)

            Divider()

            List(host.items, id: \.id) { item in
                ResultRow(item: item)
            }
            .listStyle(.plain)

            Button(action: { host.screenModel.clearList() }) {
                Text("CLEAR LIST")
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
            }
            .background(host.items.isEmpty ? Color.gray : Color.scanditBlack)
            .foregroundColor(.white)
            .cornerRadius(8)
            .padding(16)
            .disabled(host.items.isEmpty)
        }
        // The SparkScan pre-built UI (trigger button + mini preview) floats
        // over the whole screen; its container is transparent except for those
        // controls, so the list shows through.
        .overlay(
            SparkScanViewRepresentable(sparkScanView: host.sparkScanView)
                .edgesIgnoringSafeArea(.all)
        )
        // SparkScan lets the user drag the preview + trigger button around the
        // screen with a pan gesture. On a NavigationStack that pan fights the
        // interactive swipe-back-to-close gesture (both are pans). Make the
        // SparkScan pan defer to the edge swipe-back so they coexist: an
        // edge swipe closes the screen, a drag anywhere else moves the preview.
        .background(
            NavigationSwipeBackCoordinator(sparkScanView: host.sparkScanView)
        )
        .onAppear { host.sparkScanView.onResume() }
        .onDisappear { host.sparkScanView.onPause() }
    }
}

// Makes SparkScan's drag pan gesture(s) require the navigation controller's
// interactive pop (edge swipe-back) gesture to fail before they begin. The pop
// gesture is a screen-edge recognizer, so it fails instantly away from the left
// edge (drag runs with no perceptible delay) and wins at the edge (swipe-back
// closes the screen) — letting both gestures coexist cleanly (SDC-32592).
private struct NavigationSwipeBackCoordinator: UIViewControllerRepresentable {
    let sparkScanView: SparkScanView

    func makeUIViewController(context: Context) -> SwipeBackCoordinatorController {
        SwipeBackCoordinatorController(sparkScanView: sparkScanView)
    }

    func updateUIViewController(_ uiViewController: SwipeBackCoordinatorController, context: Context) {}

    final class SwipeBackCoordinatorController: UIViewController {
        private let sparkScanView: SparkScanView

        init(sparkScanView: SparkScanView) {
            self.sparkScanView = sparkScanView
            super.init(nibName: nil, bundle: nil)
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) { nil }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            guard let popGesture = navigationController?.interactivePopGestureRecognizer else {
                return
            }
            // SparkScan installs its pan gesture(s) on the SDCSparkScanView the
            // SDK adds into the KMP container (its single subview).
            let sparkScanNativeView = sparkScanView.toUIView().subviews.first
            sparkScanNativeView?.gestureRecognizers?
                .compactMap { $0 as? UIPanGestureRecognizer }
                .forEach { $0.require(toFail: popGesture) }
        }
    }
}

private struct ResultRow: View {
    let item: ListBuildingEntry

    var body: some View {
        HStack(spacing: 16) {
            ThumbnailStack(images: item.images)
                .frame(width: 70, height: 70)
                .background(Color.scanditLightGrey)

            VStack(alignment: .leading, spacing: 4) {
                Text(item.barcodeData)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color.scanditBlack)
                    .lineLimit(2)
                Text(item.symbology)
                    .font(.system(size: 12))
                    .foregroundColor(Color.scanditBlack.opacity(0.5))
            }

            Spacer()

            if item.quantity > 1 {
                Text("Qty: \(item.quantity)")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(Color.scanditBlack)
            }
        }
        .padding(.vertical, 8)
    }
}

// Up to 3 thumbnails stacked with a small peek offset — mirrors the native
// StackedImageView / the Android ThumbnailStack.
private struct ThumbnailStack: View {
    let images: [CroppedImage]

    private let peek: CGFloat = 4

    var body: some View {
        let shown = Array(images.suffix(3))
        ZStack {
            ForEach(Array(shown.enumerated()), id: \.offset) { index, image in
                let frontOffset = peek * CGFloat(shown.count - 1 - index)
                Image(uiImage: image.toUIImage())
                    .resizable()
                    .scaledToFill()
                    .frame(
                        width: 70 - peek * CGFloat(shown.count - 1),
                        height: 70 - peek * CGFloat(shown.count - 1)
                    )
                    .clipped()
                    .cornerRadius(2)
                    .offset(x: frontOffset, y: frontOffset)
            }
        }
    }
}

// UIViewRepresentable bridge embedding the SDK's SparkScanView UIView.
//
// The SparkScanView is layered full-screen over the app UI. Its underlying
// UIKit `SDCSparkScanView` is *greedy*: it returns itself from `hitTest` for
// taps anywhere in its frame — including the transparent regions where the
// app UI shows through — so a SwiftUI Button placed underneath (the CLEAR LIST
// button) never receives touches. We wrap it in a `SparkScanPassthroughView`
// that returns nil for those transparent hits so SwiftUI routes the touch to
// the underlying view, while the SparkScan controls (trigger button, preview)
// — which are deeper subviews — stay tappable. Mirrors the SDK's own SwiftUI
// integration (SparkScanSwiftUI.swift `PassthroughView`).
struct SparkScanViewRepresentable: UIViewRepresentable {
    let sparkScanView: SparkScanView

    func makeUIView(context: Context) -> UIView {
        let passthrough = SparkScanPassthroughView()
        let container = sparkScanView.toUIView()
        container.frame = passthrough.bounds
        container.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        passthrough.addSubview(container)
        passthrough.sparkScanContainer = container
        return passthrough
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        guard let passthrough = uiView as? SparkScanPassthroughView,
            let container = passthrough.subviews.first
        else { return }
        if container.frame != passthrough.bounds {
            container.frame = passthrough.bounds
        }
    }
}

// Restores SparkScanView's touch passthrough through the KMP container.
//
// `SDCSparkScanView` already decides touch routing correctly on its own: while
// the preview is collapsed (idle) its `hitTest` returns nil for empty regions
// so touches fall through to the app UI, and while the preview is expanded it
// returns itself so its tap-recognizer can collapse it on an outside tap
// (controls are deeper subviews and are always delivered). But the KMP
// `SparkScanView.toUIView()` container is a plain UIView: when its child
// returns nil, the container still returns *itself* from hitTest, re-capturing
// the touch the SparkScanView meant to pass through — so the CLEAR LIST button
// underneath never receives taps.
//
// This wrapper forwards that passthrough: when a hit resolves to the container
// itself (child declined), return nil so SwiftUI routes the touch to the
// content below. Hits that resolve to the SparkScanView (expanded → collapse
// gesture) or to a real control are delivered unchanged.
final class SparkScanPassthroughView: UIView {
    // The KMP `SparkScanView.toUIView()` container (a plain UIView wrapping the
    // SDCSparkScanView). Weak: the container is owned by its superview here.
    weak var sparkScanContainer: UIView?

    override func hitTest(_ point: CGPoint, with event: UIEvent?) -> UIView? {
        let hit = super.hitTest(point, with: event)
        if hit === self || hit === sparkScanContainer {
            return nil
        }
        return hit
    }
}
