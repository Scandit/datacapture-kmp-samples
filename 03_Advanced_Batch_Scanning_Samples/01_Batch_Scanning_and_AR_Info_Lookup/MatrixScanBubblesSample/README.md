# MatrixScan Bubbles Sample

This sample uses the camera to track multiple barcodes at once (MatrixScan / BarcodeBatch) and shows an augmented-reality "bubble" over each barcode that is large enough on screen. Each bubble displays back-end stock data for the scanned code; tap a bubble to flip it to the raw barcode value. Tap the freeze button to pause the camera while keeping the bubbles on screen.

This is a Kotlin Multiplatform sample sharing the BarcodeBatch wiring and freeze state machine in `shared/`, with thin Compose (Android) and SwiftUI (iOS) hosts that embed the camera view and build the native bubble view.

## Installation

- Clone this repo locally.
- Sign in to your Developer Account at [ssl.scandit.com](http://ssl.scandit.com) and generate a license key. If you do not have an account, sign up here: [https://ssl.scandit.com/dashboard/sign-up?p=test](https://ssl.scandit.com/dashboard/sign-up?p=test).
- Replace the license key in the sample where you see `-- ENTER YOUR SCANDIT LICENSE KEY HERE --`.
- Build and run this sample on your mobile device.

## Documentation

BarcodeBatch (MatrixScan) is a fully-flexible API of the Scandit Data Capture SDK. Our SDK is supported on most popular frameworks.

[iOS](https://docs.scandit.com/data-capture-sdk/ios/index.html), [Android](https://docs.scandit.com/data-capture-sdk/android/index.html), [Web](https://docs.scandit.com/data-capture-sdk/web/index.html), [Cordova](https://docs.scandit.com/data-capture-sdk/cordova/index.html), .NET ([iOS](https://docs.scandit.com/data-capture-sdk/dotnet.ios/index.html), [Android](https://docs.scandit.com/data-capture-sdk/dotnet.android/index.html)), [React Native](https://docs.scandit.com/data-capture-sdk/react-native/index.html), [Flutter](https://docs.scandit.com/data-capture-sdk/flutter/index.html), [Capacitor](https://docs.scandit.com/data-capture-sdk/capacitor/index.html), [Titanium](https://docs.scandit.com/data-capture-sdk/titanium/index.html)

## Sample Barcodes

Once you get the sample up and running, go find some barcodes to scan. Don’t feel like getting up from your desk? Here’s a [handy pdf of barcodes](https://github.com/Scandit/.github/blob/main/images/PrintTheseBarcodes.pdf) you can print out.

## Trial Signup

To add MatrixScan to your app, sign up for your Scandit Developer Account and get instant access to your license key: [https://ssl.scandit.com/dashboard/sign-up?p=test](https://ssl.scandit.com/dashboard/sign-up?p=test)

## Support

Our support engineers can be reached at [support@scandit.com](mailto:support@scandit.com).

## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
