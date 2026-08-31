/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

import Combine
import SwiftUI
import shared

// Scanner host: state holder + full-screen camera preview view + the
// UIViewRepresentable bridge that embeds the SDK's `UIView` into SwiftUI.
// The shared screen model owns all SDK wiring and the freeze state machine;
// this host only embeds the view and builds the per-barcode bubble UIView.

private extension DataCaptureView {
    var hostView: UIView { toUIView() }
}

// State-owned holder that pins the shared screen model + its flow observer to
// this view's lifetime. SwiftUI re-runs `MatrixScanBubblesHost.init()` on every
// body recomputation, so a plain stored property would construct a fresh model
// each time. Routing both through a single `@StateObject` keeps observer and
// actions pointing at the same instance.
final class MatrixScanBubblesHostState: ObservableObject {
    let screenModel: MatrixScanBubblesScreenModel
    @Published var frozen: Bool = false

    private let observer: FrozenObserver
    private var cancellable: AnyCancellable?

    init() {
        let model = MatrixScanBubblesScreenModel()
        self.screenModel = model
        self.observer = FrozenObserver(model: model)
        self.frozen = observer.value
        // `sink` with `[weak self]` — `assign(to:on:)` would retain `self`,
        // forming a cycle (self holds the AnyCancellable) that blocks `deinit`
        // and so `screenModel.dispose()` would never run.
        self.cancellable = observer.$value
            .receive(on: DispatchQueue.main)
            .sink { [weak self] in self?.frozen = $0 }
    }

    deinit {
        screenModel.dispose()
    }
}

struct MatrixScanBubblesHost: View {
    @StateObject private var host = MatrixScanBubblesHostState()

    var body: some View {
        ZStack {
            DataCaptureViewRepresentable {
                let dcView = host.screenModel.setupDataCaptureView(
                    view: DataCaptureView(dataCaptureContext: host.screenModel.dataCaptureContext),
                    bubbleViewFactory: { trackedBarcode, bubbleData in
                        BubbleView.make(code: trackedBarcode.barcode.data ?? "", data: bubbleData)
                    }
                )
                return dcView.hostView
            }
            .edgesIgnoringSafeArea(.all)

            // Freeze toggle: a 120pt transparent image button centred at the
            // bottom, matching the Android sample's freeze icons —
            // freeze_enabled (plain circle) while scanning, freeze_disabled
            // (circle with ✕) once frozen.
            VStack {
                Spacer()
                Button(action: { host.screenModel.onEvent(event: .toggleFreeze) }) {
                    Image(host.frozen ? "freeze_disabled" : "freeze_enabled")
                        .resizable()
                        .frame(width: 120, height: 120)
                }
                .padding(.bottom, 24)
            }
        }
        .onAppear { host.screenModel.onEvent(event: .started) }
        .onDisappear { host.screenModel.onEvent(event: .stopped) }
    }
}

struct DataCaptureViewRepresentable: UIViewRepresentable {
    let makeView: () -> UIView

    func makeUIView(context: Context) -> UIView { makeView() }

    func updateUIView(_ uiView: UIView, context: Context) {
        if let superview = uiView.superview, uiView.frame != superview.bounds {
            uiView.frame = superview.bounds
        }
    }
}

// Builds the AR bubble UIView shown over a tracked barcode. Tapping it toggles
// between the back-end stock data and the raw barcode code, mirroring the
// native MatrixScanBubblesSample.
final class BubbleView: UIView {
    private let reportLabel = UILabel()
    private let shelfLabel = UILabel()
    private let codeLabel = UILabel()
    private var showShelfData = true

    static func make(code: String, data: BubbleData) -> UIView {
        let bubble = BubbleView()
        bubble.configure(code: code, data: data)
        return bubble
    }

    private func configure(code: String, data: BubbleData) {
        backgroundColor = UIColor(white: 0.87, alpha: 1)
        layer.cornerRadius = 20
        translatesAutoresizingMaskIntoConstraints = false

        reportLabel.text = "Report stock count"
        reportLabel.textColor = .black
        shelfLabel.text = "Shelf: \(data.shelfCount) Back room: \(data.backroomCount)"
        shelfLabel.textColor = .black
        codeLabel.text = code
        codeLabel.textColor = .black
        codeLabel.isHidden = true

        let stack = UIStackView(arrangedSubviews: [reportLabel, shelfLabel, codeLabel])
        stack.axis = .vertical
        stack.alignment = .center
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)
        NSLayoutConstraint.activate([
            stack.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            stack.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -8),
            stack.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
        ])

        addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(toggle)))
    }

    @objc private func toggle() {
        showShelfData.toggle()
        reportLabel.isHidden = !showShelfData
        shelfLabel.isHidden = !showShelfData
        codeLabel.isHidden = showShelfData
    }
}
