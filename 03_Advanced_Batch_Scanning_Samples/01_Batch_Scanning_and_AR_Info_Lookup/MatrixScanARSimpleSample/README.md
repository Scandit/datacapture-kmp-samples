# MatrixScan AR Simple Sample

This sample uses the camera to track multiple barcodes at once (MatrixScan AR / BarcodeAr) and overlays augmented-reality content on top of each tracked barcode. Pick a mode from the menu to see four different ways of presenting AR content:

- **Highlights** — a tappable rectangular highlight per barcode; tapping toggles a selected/unselected style.
- **Annotations** — a circular highlight plus an info annotation that swaps between a compact far-away card and a detailed close-up card (header, footer, body, tap-to-recolor).
- **Popovers** — a circular icon highlight plus an Accept/Reject popover for barcodes flagged as needing review.
- **Status Icons** — a rectangular highlight plus a status-icon annotation flagging barcodes as "Close to expiry" or "Item expired".

This is a Kotlin Multiplatform sample sharing the BarcodeAr wiring and per-mode highlight/annotation behaviours in `shared/`, with thin Compose (Android) and SwiftUI (iOS) hosts that embed the camera view.

## Installation

- Clone this repo locally.
- Sign in to your Developer Account at [ssl.scandit.com](http://ssl.scandit.com) and generate a license key. If you do not have an account, sign up here: [https://ssl.scandit.com/dashboard/sign-up?p=test](https://ssl.scandit.com/dashboard/sign-up?p=test).
- Replace the license key in the sample where you see `-- ENTER YOUR SCANDIT LICENSE KEY HERE --`.
- Build and run this sample on your mobile device.

## Documentation

BarcodeAr (MatrixScan AR) is a fully-flexible API of the Scandit Data Capture SDK. Our SDK is supported on most popular frameworks.

[iOS](https://docs.scandit.com/data-capture-sdk/ios/index.html), [Android](https://docs.scandit.com/data-capture-sdk/android/index.html), [Web](https://docs.scandit.com/data-capture-sdk/web/index.html), [Cordova](https://docs.scandit.com/data-capture-sdk/cordova/index.html), .NET ([iOS](https://docs.scandit.com/data-capture-sdk/dotnet.ios/index.html), [Android](https://docs.scandit.com/data-capture-sdk/dotnet.android/index.html)), [React Native](https://docs.scandit.com/data-capture-sdk/react-native/index.html), [Flutter](https://docs.scandit.com/data-capture-sdk/flutter/index.html), [Capacitor](https://docs.scandit.com/data-capture-sdk/capacitor/index.html), [Titanium](https://docs.scandit.com/data-capture-sdk/titanium/index.html)

## Sample Barcodes

Once you get the sample up and running, go find some barcodes to scan. Don’t feel like getting up from your desk? Here’s a [handy pdf of barcodes](https://github.com/Scandit/.github/blob/main/images/PrintTheseBarcodes.pdf) you can print out.

## Trial Signup

To add MatrixScan to your app, sign up for your Scandit Developer Account and get instant access to your license key: [https://ssl.scandit.com/dashboard/sign-up?p=test](https://ssl.scandit.com/dashboard/sign-up?p=test)

## Support

Our support engineers can be reached at [support@scandit.com](mailto:support@scandit.com).

## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
