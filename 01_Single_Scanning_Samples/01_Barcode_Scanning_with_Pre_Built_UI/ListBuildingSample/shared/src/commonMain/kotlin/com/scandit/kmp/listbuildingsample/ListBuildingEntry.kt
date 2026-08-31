/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.listbuildingsample

/**
 * One row in the scanned-barcode list. Mirrors the native Android sample's
 * `ScanResult`: duplicate scans of the same barcode (same data + symbology)
 * increment [quantity] and append a fresh thumbnail, capping the stack at
 * [MAX_IMAGES] (newest kept, oldest dropped).
 *
 * Immutable by design — the screen model rebuilds the list on every change so
 * Compose / SwiftUI observe a new value and recompose. [withAdditionalScan]
 * returns an updated copy rather than mutating in place.
 */
data class ListBuildingEntry(
    val barcodeData: String,
    val symbology: String,
    val quantity: Int,
    val images: List<CroppedImage>,
) {
    /**
     * Stable list identity. A row is keyed on data **and** symbology (the same
     * data scanned under two symbologies is two distinct rows), so the list
     * identity must be the composite — using [barcodeData] alone produces
     * duplicate IDs that SwiftUI's `List` will not tolerate. Matches the dedup
     * key in [ScannedItemList] and the Android host's `"${'$'}data${'$'}symbology"`.
     */
    val id: String get() = "$barcodeData$symbology"

    /**
     * Returns a copy with the quantity incremented and [newImage] appended,
     * keeping at most [MAX_IMAGES] thumbnails (drop-oldest).
     */
    fun withAdditionalScan(newImage: CroppedImage?): ListBuildingEntry {
        val nextImages = if (newImage != null) {
            (images + newImage).takeLast(MAX_IMAGES)
        } else {
            images
        }
        return copy(quantity = quantity + 1, images = nextImages)
    }

    companion object {
        const val MAX_IMAGES = 3

        /** Builds the first occurrence of a freshly-scanned barcode. */
        fun firstScan(
            barcodeData: String,
            symbology: String,
            image: CroppedImage?,
        ): ListBuildingEntry = ListBuildingEntry(
            barcodeData = barcodeData,
            symbology = symbology,
            quantity = 1,
            images = listOfNotNull(image),
        )
    }
}
