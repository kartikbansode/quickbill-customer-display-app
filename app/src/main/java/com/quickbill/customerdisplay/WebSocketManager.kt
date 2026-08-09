package com.quickbill.customerdisplay

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketManager(
    private val onConnectionChanged: (Boolean) -> Unit,
    private val onBillUpdate: (BillState) -> Unit,
    private val onPaymentUpdate: (PaymentState) -> Unit,
    private val onSaleCompleted: (PaymentState) -> Unit,
    private val onPaymentCancelled: (PaymentState) -> Unit,
    private val onNewBill: (BillState) -> Unit,
    private val onError: (String) -> Unit,
    private val onDesktopDiscovered: ((String, Int) -> Unit)? = null
) {

    companion object {

        private const val DEFAULT_PORT = 8765

        private const val DISCOVERY_PORT = 8766

        private const val DISCOVERY_REQUEST =
            "QUICKBILL_DISCOVER_V1"

        private const val DISCOVERY_RESPONSE =
            "quickbill_discovery_response"

        private const val DISCOVERY_TIMEOUT_MS = 1500

        private const val RECONNECT_MIN_MS = 1000L

        private const val RECONNECT_MAX_MS = 10000L
    }

    private val client =
        OkHttpClient.Builder()
            .readTimeout(
                0,
                TimeUnit.MILLISECONDS
            )
            .pingInterval(
                15,
                TimeUnit.SECONDS
            )
            .build()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var currentIp: String? = null

    @Volatile
    private var currentPort: Int = DEFAULT_PORT

    private val running =
        AtomicBoolean(false)

    private val connected =
        AtomicBoolean(false)

    @Volatile
    private var reconnectDelay =
        RECONNECT_MIN_MS

    @Volatile
    private var reconnectThread: Thread? = null

    @Volatile
    private var discoveryThread: Thread? = null

    private val lifecycleLock =
        Any()

    // =========================================================
    // START
    // =========================================================

    fun start(
        preferredIp: String?,
        port: Int = DEFAULT_PORT
    ) {

        if (!running.compareAndSet(false, true)) {
            return
        }

        currentIp =
            preferredIp
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }

        currentPort = port

        reconnectDelay =
            RECONNECT_MIN_MS

        connectCurrent()
    }

    // =========================================================
    // NORMAL / MANUAL CONNECT
    // =========================================================

    fun connect(
        ipAddress: String,
        port: Int = DEFAULT_PORT
    ) {

        currentIp =
            ipAddress
                .trim()
                .takeIf {
                    it.isNotEmpty()
                }

        currentPort = port

        running.set(true)

        reconnectDelay =
            RECONNECT_MIN_MS

        connectCurrent()
    }

    // =========================================================
    // CONNECT TO CURRENT DESKTOP
    // =========================================================

    private fun connectCurrent() {

        if (!running.get()) {
            return
        }

        val ip = currentIp

        if (ip.isNullOrBlank()) {

            startDiscovery()

            return
        }

        connectTo(
            ip,
            currentPort
        )
    }

    // =========================================================
    // WEBSOCKET CONNECTION
    // =========================================================

    private fun connectTo(
        ipAddress: String,
        port: Int
    ) {

        if (!running.get()) {
            return
        }

        val url =
            "ws://$ipAddress:$port"

        val request =
            Request.Builder()
                .url(url)
                .build()

        try {

            webSocket?.cancel()

        } catch (_: Exception) {
        }

        try {

            webSocket =
                client.newWebSocket(
                    request,
                    object : WebSocketListener() {

                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response
                        ) {

                            if (!running.get()) {
                                webSocket.close(
                                    1000,
                                    "Client stopped"
                                )
                                return
                            }

                            this@WebSocketManager.webSocket =
                                webSocket

                            connected.set(true)

                            reconnectDelay =
                                RECONNECT_MIN_MS

                            onConnectionChanged(
                                true
                            )

                            val hello =
                                JSONObject()
                                    .put(
                                        "type",
                                        "hello"
                                    )
                                    .put(
                                        "app",
                                        "QuickBill"
                                    )
                                    .put(
                                        "client",
                                        "android_customer_display"
                                    )
                                    .put(
                                        "version",
                                        2
                                    )

                            webSocket.send(
                                hello.toString()
                            )
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String
                        ) {

                            handleMessage(
                                text
                            )
                        }

                        override fun onClosing(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String
                        ) {

                            connected.set(false)

                            onConnectionChanged(
                                false
                            )

                            webSocket.close(
                                code,
                                reason
                            )

                            scheduleReconnect()
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String
                        ) {

                            connected.set(false)

                            onConnectionChanged(
                                false
                            )

                            scheduleReconnect()
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?
                        ) {

                            connected.set(false)

                            onConnectionChanged(
                                false
                            )

                            if (running.get()) {

                                onError(
                                    t.message
                                        ?: "Connection failed"
                                )
                            }

                            scheduleReconnect()
                        }
                    }
                )

        } catch (exception: Exception) {

            connected.set(false)

            onConnectionChanged(
                false
            )

            if (running.get()) {

                onError(
                    exception.message
                        ?: "Unable to connect"
                )
            }

            scheduleReconnect()
        }
    }

    // =========================================================
    // AUTOMATIC RECONNECT
    // =========================================================

    private fun scheduleReconnect() {

        if (!running.get()) {
            return
        }

        synchronized(
            lifecycleLock
        ) {

            if (
                reconnectThread
                    ?.isAlive == true
            ) {
                return
            }

            val delay =
                reconnectDelay

            reconnectDelay =
                (
                        reconnectDelay * 2
                        ).coerceAtMost(
                        RECONNECT_MAX_MS
                    )

            reconnectThread =
                Thread {

                    try {

                        Thread.sleep(
                            delay
                        )

                    } catch (_: InterruptedException) {

                        return@Thread
                    }

                    if (!running.get()) {
                        return@Thread
                    }

                    if (connected.get()) {
                        return@Thread
                    }

                    // First try the last known PC.
                    connectCurrent()

                    // Also search the LAN in case
                    // the PC IP has changed.
                    startDiscovery()

                }.apply {

                    name =
                        "QuickBill-Reconnect"

                    isDaemon = true

                    start()
                }
        }
    }

    // =========================================================
    // LAN DISCOVERY
    // =========================================================

    private fun startDiscovery() {

        if (!running.get()) {
            return
        }

        synchronized(
            lifecycleLock
        ) {

            if (
                discoveryThread
                    ?.isAlive == true
            ) {
                return
            }

            discoveryThread =
                Thread {

                    discoverDesktop()

                }.apply {

                    name =
                        "QuickBill-Desktop-Discovery"

                    isDaemon = true

                    start()
                }
        }
    }

    private fun discoverDesktop() {

        var socket:
                DatagramSocket? = null

        try {

            socket =
                DatagramSocket()

            socket.broadcast = true

            socket.soTimeout =
                DISCOVERY_TIMEOUT_MS

            val requestBytes =
                DISCOVERY_REQUEST
                    .toByteArray(
                        Charsets.UTF_8
                    )

            val broadcastAddress =
                InetAddress.getByName(
                    "255.255.255.255"
                )

            val requestPacket =
                DatagramPacket(
                    requestBytes,
                    requestBytes.size,
                    broadcastAddress,
                    DISCOVERY_PORT
                )

            socket.send(
                requestPacket
            )

            val buffer =
                ByteArray(2048)

            while (
                running.get()
                && !connected.get()
            ) {

                try {

                    val responsePacket =
                        DatagramPacket(
                            buffer,
                            buffer.size
                        )

                    socket.receive(
                        responsePacket
                    )

                    val response =
                        String(
                            responsePacket.data,
                            responsePacket.offset,
                            responsePacket.length,
                            Charsets.UTF_8
                        )

                    val json =
                        JSONObject(
                            response
                        )

                    if (
                        json.optString(
                            "type"
                        ) != DISCOVERY_RESPONSE
                    ) {
                        continue
                    }

                    val ip =
                        json.optString(
                            "ip",
                            ""
                        ).trim()

                    val port =
                        json.optInt(
                            "port",
                            DEFAULT_PORT
                        )

                    if (ip.isBlank()) {
                        continue
                    }

                    currentIp = ip

                    currentPort = port

                    onDesktopDiscovered?.invoke(
                        ip,
                        port
                    )

                    connectTo(
                        ip,
                        port
                    )

                    break

                } catch (
                    _: SocketTimeoutException
                ) {

                    break
                }
            }

        } catch (exception: Exception) {

            if (running.get()) {

                onError(
                    "Desktop discovery failed: " +
                            (
                                    exception.message
                                        ?: "Unknown error"
                                    )
                )
            }

        } finally {

            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    // =========================================================
    // DISCONNECT
    // =========================================================

    fun disconnect() {

        running.set(false)

        connected.set(false)

        try {

            webSocket?.close(
                1000,
                "Client closing"
            )

        } catch (_: Exception) {
        }

        try {

            webSocket?.cancel()

        } catch (_: Exception) {
        }

        webSocket = null

        synchronized(
            lifecycleLock
        ) {

            reconnectThread
                ?.interrupt()

            reconnectThread = null

            discoveryThread
                ?.interrupt()

            discoveryThread = null
        }

        onConnectionChanged(
            false
        )
    }

    // =========================================================
    // MESSAGE HANDLING
    // =========================================================

    private fun handleMessage(
        text: String
    ) {

        try {

            val json =
                JSONObject(text)

            when (
                json.optString(
                    "type"
                )
            ) {

                "hello_ack" -> {

                    connected.set(
                        true
                    )

                    onConnectionChanged(
                        true
                    )
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

                "payment_cancelled" -> {

                    onPaymentCancelled(
                        parsePayment(
                            json,
                            PaymentStatus.CANCELLED
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

        } catch (exception: Exception) {

            onError(
                "Invalid server message: " +
                        (
                                exception.message
                                    ?: "Unknown error"
                                )
            )
        }
    }

    // =========================================================
    // BILL PARSER
    // =========================================================

    private fun parseBill(
        json: JSONObject
    ): BillState {

        val customerJson =
            json.optJSONObject(
                "customer"
            )

        val customer =
            Customer(
                name =
                    customerJson
                        ?.optString(
                            "name",
                            "Walk-in Customer"
                        )
                        ?: "Walk-in Customer",

                mobile =
                    customerJson
                        ?.optString(
                            "mobile",
                            ""
                        )
                        ?: ""
            )

        val itemsJson =
            json.optJSONArray(
                "items"
            )
                ?: JSONArray()

        val items =
            mutableListOf<BillItem>()

        for (
        i in 0 until itemsJson.length()
        ) {

            val item =
                itemsJson.optJSONObject(i)
                    ?: continue

            items.add(
                BillItem(

                    barcode =
                        item.optString(
                            "barcode",
                            ""
                        ),

                    sku =
                        item.optString(
                            "sku",
                            ""
                        ),

                    name =
                        item.optString(
                            "name",
                            ""
                        ),

                    brand =
                        item.optString(
                            "brand",
                            ""
                        ),

                    category =
                        item.optString(
                            "category",
                            ""
                        ),

                    qty =
                        item.optInt(
                            "qty",
                            0
                        ),

                    rate =
                        item.optDouble(
                            "rate",
                            0.0
                        ),

                    amount =
                        item.optDouble(
                            "amount",
                            0.0
                        ),

                    gst =
                        item.optDouble(
                            "gst",
                            0.0
                        )
                )
            )
        }

        return BillState(

            billNo =
                json.optString(
                    "bill_no",
                    ""
                ),

            customer =
                customer,

            cashier =
                json.optString(
                    "cashier",
                    "Admin"
                ),

            items =
                items,

            subtotal =
                json.optDouble(
                    "subtotal",
                    0.0
                ),

            tax =
                json.optDouble(
                    "tax",
                    0.0
                ),

            discount =
                json.optDouble(
                    "discount",
                    0.0
                ),

            total =
                json.optDouble(
                    "total",
                    0.0
                )
        )
    }

    // =========================================================
    // PAYMENT PARSER
    // =========================================================

    private fun parsePayment(
        json: JSONObject,
        status: PaymentStatus
    ): PaymentState {

        val payment =
            json.optJSONObject(
                "payment"
            )
                ?: JSONObject()

        val mode =
            when (
                payment.optString(
                    "mode",
                    ""
                ).uppercase()
            ) {

                "CASH" ->
                    PaymentMode.CASH

                "UPI" ->
                    PaymentMode.UPI

                "CARD" ->
                    PaymentMode.CARD

                "CREDIT" ->
                    PaymentMode.CREDIT

                else ->
                    PaymentMode.UNKNOWN
            }

        val qr =
            payment.optJSONObject(
                "qr"
            )

        return PaymentState(

            status =
                status,

            mode =
                mode,

            total =
                payment.optDouble(
                    "total",
                    0.0
                ),

            qrEnabled =
                qr?.optBoolean(
                    "enabled",
                    false
                ) ?: false,

            qrAmount =
                qr?.optDouble(
                    "amount",
                    payment.optDouble(
                        "total",
                        0.0
                    )
                ) ?: 0.0,

            upiId =
                qr?.optString(
                    "upi_id",
                    ""
                ) ?: "",

            qrPayload =
                qr?.optString(
                    "payload",
                    ""
                ) ?: "",

            merchantName =
                qr?.optString(
                    "merchant_name",
                    "QuickBill"
                ) ?: "QuickBill"
        )
    }
}