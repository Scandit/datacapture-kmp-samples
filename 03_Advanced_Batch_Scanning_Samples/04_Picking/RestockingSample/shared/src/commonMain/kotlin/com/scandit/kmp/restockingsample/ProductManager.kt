/*
 * This file is part of the Scandit Data Capture SDK
 *
 * Copyright (C) 2026- Scandit AG. All rights reserved.
 */

package com.scandit.kmp.restockingsample

import com.kmp.datacapture.barcode.pick.BarcodePickProduct
import com.kmp.datacapture.barcode.pick.BarcodePickProductProviderCallback
import com.kmp.datacapture.barcode.pick.BarcodePickProductProviderCallbackItem

// The list item shapes rendered by the result screen: a section header
// (Picklist / Inventory, with a live count) followed by its product rows.
// Mirrors the native sample's `Header` + `DisplayProduct`.
//
// The subclasses are TOP-LEVEL (not nested in [ListItem]) on purpose:
// Kotlin/Native exports top-level classes to Swift under their own name
// (e.g. `RestockingHeader`), whereas nested sealed subclasses are not
// reachable from Swift by an `OuterInner` name. Every sibling KMP sample
// uses top-level data classes for list content for the same reason.
sealed interface ListItem

data class RestockingHeader(val isPickList: Boolean, val count: Int) : ListItem

data class RestockingProductRow(
    val identifier: String,
    val quantityToPick: Int,
    val barcodeData: String,
    val picked: Boolean,
) : ListItem

// Helper class to query the list of products and to maintain the list of
// items that are picked and scanned. A real application would implement a
// database and network call; here a fixed in-memory product list simulates it
// (mirrors the native sample's ProductDatabase).
class ProductManager {

    private val products: List<Product> = listOf(
        Product(
            "Item A",
            1,
            listOf("8414792869912", "3714711193285", "4951107312342", "1520070879331"),
        ),
        Product("Item B", 1, listOf("1318890267144", "9866064348233", "4782150677788", "2371266523793")),
        Product("Item C", 1, listOf("5984430889778", "7611879254123")),
    )

    private var allScannedCodes: Set<String> = emptySet()
    private var allPickedCodes: Set<String> = emptySet()

    fun getProductForItemData(itemData: String): Product? =
        products.firstOrNull { itemData in it.barcodeData }

    // Confirm a picked/unpicked item against the "database". A real app would
    // hit a local DB or backend here; the native sample always returns true.
    fun verifyProductInDatabase(itemData: String): Boolean = true

    // Creates the list of [BarcodePickProduct] that we need to pick, directly
    // ingestible when creating the [com.kmp.datacapture.barcode.pick.BarcodePick] mode.
    fun getBarcodePickProducts(): Set<BarcodePickProduct> =
        products.map { BarcodePickProduct(it.identifier, it.quantityToPick) }.toSet()

    // Asynchronously match scanned barcode content with our products, so the
    // BarcodePick UI knows whether a scanned item belongs to the pick list.
    fun mapItems(itemsData: Set<String>, callback: BarcodePickProductProviderCallback) {
        val mapped = itemsData.map { itemData ->
            val product = getProductForItemData(itemData)
            BarcodePickProductProviderCallbackItem(itemData, product?.identifier)
        }.toSet()
        callback.onData(mapped)
    }

    fun setAllScannedCodes(scannedCodes: Set<String>) {
        allScannedCodes = scannedCodes
    }

    fun setAllPickedCodes(pickedCodes: Set<String>) {
        allPickedCodes = pickedCodes
    }

    /** Clear all picked and scanned codes so we can start the sample over again. */
    fun clearPickedAndScanned() {
        allPickedCodes = emptySet()
        allScannedCodes = emptySet()
    }

    // For display purposes only: builds the content of the result screen —
    // the Picklist section (everything picked) followed by the Inventory
    // section (scanned but not picked). Mirrors the native sample's
    // getPickResultContent().
    fun getPickResultContent(): List<ListItem> =
        buildPickedProductsListAndSetPickedProductsCount() + getInventoryList()

    private fun buildPickedProductsListAndSetPickedProductsCount(): List<ListItem> {
        val rows = mutableListOf<RestockingProductRow>()
        val pickedProducts = mutableListOf<Product>()

        for (item in allPickedCodes) {
            val product = getProductForItemData(item)
            if (product != null) {
                pickedProducts.add(product)
                val numberOfProductsInList = pickedProducts.count { it == product }
                // Only show with a checkmark if the number of same products in the
                // displayed list is less than or equal to the quantity to pick.
                val picked = numberOfProductsInList <= product.quantityToPick
                rows.add(RestockingProductRow(product.identifier, product.quantityToPick, item, picked))
            } else {
                // No product matching the item was found.
                rows.add(RestockingProductRow("Unknown", 0, item, true))
            }
        }

        val header = RestockingHeader(isPickList = true, count = products.size)
        return listOf(header) + rows
    }

    private fun getInventoryList(): List<ListItem> {
        val scannedNotPicked = allScannedCodes - allPickedCodes
        val header = RestockingHeader(isPickList = false, count = scannedNotPicked.size)

        val rows = scannedNotPicked.map { item ->
            val product = getProductForItemData(item)
            if (product == null) {
                RestockingProductRow("Unknown", 0, item, picked = false)
            } else {
                RestockingProductRow(product.identifier, product.quantityToPick, item, picked = false)
            }
        }

        return listOf(header) + rows
    }
}
