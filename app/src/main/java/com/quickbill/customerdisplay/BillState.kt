package com.quickbill.customerdisplay

data class BillItem(
    val barcode: String = "",
    val sku: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val qty: Int = 0,
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val gst: Double = 0.0
)

data class Customer(
    val name: String = "Walk-in Customer",
    val mobile: String = ""
)

data class BillState(
    val billNo: String = "",
    val customer: Customer = Customer(),
    val cashier: String = "Admin",
    val items: List<BillItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0
)