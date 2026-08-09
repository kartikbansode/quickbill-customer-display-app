package com.quickbill.customerdisplay

enum class PaymentMode {
    CASH,
    UPI,
    CARD,
    CREDIT,
    UNKNOWN
}

enum class PaymentStatus {
    IDLE,
    STARTED,
    PENDING,
    COMPLETED,
    CANCELLED
}

data class PaymentState(
    val status: PaymentStatus = PaymentStatus.IDLE,
    val mode: PaymentMode = PaymentMode.UNKNOWN,
    val total: Double = 0.0,

    // UPI QR information supplied by QuickBill desktop
    val qrEnabled: Boolean = false,
    val qrAmount: Double = 0.0,
    val upiId: String = "",
    val qrPayload: String = "",
    val merchantName: String = "QuickBill"
)