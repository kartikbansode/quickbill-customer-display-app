package com.quickbill.customerdisplay

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val onConnectionChanged: (Boolean) -> Unit,
    private val onBillUpdate: (BillState) -> Unit,
    private val onPaymentUpdate: (PaymentState) -> Unit,
    private val onSaleCompleted: (PaymentState) -> Unit,
    private val onNewBill: (BillState) -> Unit,
    private val onError: (String) -> Unit
) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect(ipAddress: String, port: Int = 8765) {

        disconnect()

        val url = "ws://$ipAddress:$port"

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {

                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response
                ) {
                    onConnectionChanged(true)

                    val hello = JSONObject()
                        .put("type", "hello")
                        .put("app", "QuickBill")
                        .put("client", "android_customer_display")
                        .put("version", 1)

                    webSocket.send(hello.toString())
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String
                ) {
                    handleMessage(text)
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    onConnectionChanged(false)

                    webSocket.close(
                        code,
                        reason
                    )
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String
                ) {
                    onConnectionChanged(false)
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?
                ) {
                    onConnectionChanged(false)
                    onError(t.message ?: "Connection failed")
                }
            }
        )
    }

    fun disconnect() {
        webSocket?.close(
            1000,
            "Client closing"
        )

        webSocket = null
    }

    private fun handleMessage(text: String) {

        try {

            val json = JSONObject(text)

            when (json.optString("type")) {

                "hello_ack" -> {
                    onConnectionChanged(true)
                }

                "bill_update" -> {
                    onBillUpdate(
                        parseBill(json)
                    )
                }

                "payment_started" -> {
                    onPaymentUpdate(
                        parsePayment(
                            json,
                            PaymentStatus.STARTED
                        )
                    )
                }

                "payment_pending" -> {
                    onPaymentUpdate(
                        parsePayment(
                            json,
                            PaymentStatus.PENDING
                        )
                    )
                }

                "payment_completed" -> {
                    onPaymentUpdate(
                        parsePayment(
                            json,
                            PaymentStatus.COMPLETED
                        )
                    )
                }

                "sale_completed" -> {
                    onSaleCompleted(
                        parsePayment(
                            json,
                            PaymentStatus.COMPLETED
                        )
                    )
                }

                "new_bill" -> {
                    onNewBill(
                        parseBill(json)
                    )
                }
            }

        } catch (e: Exception) {

            onError(
                "Invalid server message: ${e.message}"
            )
        }
    }

    private fun parseBill(
        json: JSONObject
    ): BillState {

        val customerJson =
            json.optJSONObject("customer")

        val customer = Customer(
            name = customerJson
                ?.optString(
                    "name",
                    "Walk-in Customer"
                )
                ?: "Walk-in Customer",

            mobile = customerJson
                ?.optString(
                    "mobile",
                    ""
                )
                ?: ""
        )

        val itemsJson =
            json.optJSONArray("items")
                ?: JSONArray()

        val items = mutableListOf<BillItem>()

        for (i in 0 until itemsJson.length()) {

            val item =
                itemsJson.optJSONObject(i)
                    ?: continue

            items.add(
                BillItem(
                    barcode = item.optString(
                        "barcode",
                        ""
                    ),

                    sku = item.optString(
                        "sku",
                        ""
                    ),

                    name = item.optString(
                        "name",
                        ""
                    ),

                    brand = item.optString(
                        "brand",
                        ""
                    ),

                    category = item.optString(
                        "category",
                        ""
                    ),

                    qty = item.optInt(
                        "qty",
                        0
                    ),

                    rate = item.optDouble(
                        "rate",
                        0.0
                    ),

                    amount = item.optDouble(
                        "amount",
                        0.0
                    ),

                    gst = item.optDouble(
                        "gst",
                        0.0
                    )
                )
            )
        }

        return BillState(

            billNo = json.optString(
                "bill_no",
                ""
            ),

            customer = customer,

            cashier = json.optString(
                "cashier",
                "Admin"
            ),

            items = items,

            subtotal = json.optDouble(
                "subtotal",
                0.0
            ),

            tax = json.optDouble(
                "tax",
                0.0
            ),

            discount = json.optDouble(
                "discount",
                0.0
            ),

            total = json.optDouble(
                "total",
                0.0
            )
        )
    }

    private fun parsePayment(
        json: JSONObject,
        status: PaymentStatus
    ): PaymentState {

        val payment =
            json.optJSONObject("payment")
                ?: JSONObject()

        val mode = when (
            payment.optString(
                "mode",
                ""
            ).uppercase()
        ) {

            "CASH" -> PaymentMode.CASH
            "UPI" -> PaymentMode.UPI
            "CARD" -> PaymentMode.CARD
            "CREDIT" -> PaymentMode.CREDIT

            else -> PaymentMode.UNKNOWN
        }

        val qr =
            payment.optJSONObject("qr")

        return PaymentState(

            status = status,

            mode = mode,

            total = payment.optDouble(
                "total",
                0.0
            ),

            qrEnabled = qr
                ?.optBoolean(
                    "enabled",
                    false
                )
                ?: false,

            qrAmount = qr
                ?.optDouble(
                    "amount",
                    0.0
                )
                ?: 0.0
        )
    }
}