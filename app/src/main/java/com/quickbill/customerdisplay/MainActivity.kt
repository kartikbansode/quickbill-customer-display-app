package com.quickbill.customerdisplay

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickbill.customerdisplay.ui.theme.QuickBillCustomerDisplayTheme
import kotlinx.coroutines.delay
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.withFrameNanos

/* ============================================================
   SCREEN STATES
   ============================================================ */

private enum class DisplayScreen {
    WELCOME,
    BILL,
    PAYMENT,
    SUCCESS
}

/* ============================================================
   DESIGN TOKENS
   ============================================================ */

private val ColorInk          = Color(0xFF080C10)
private val ColorSurface      = Color(0xFF0E1318)
private val ColorWhite        = Color(0xFFFFFFFF)
private val ColorOffWhite     = Color(0xFFF2F4F7)
private val ColorMuted        = Color(0xFF8A95A3)
private val ColorSubtle       = Color(0xFF3A4450)

private val ColorSuccess      = Color(0xFF22C55E)
private val ColorSuccessDark  = Color(0xFF052912)
private val ColorSuccessMid   = Color(0xFF0D3A1A)

private val ColorUpiGreen     = Color(0xFF00B86B)
private val ColorUpiDark      = Color(0xFF041C10)
private val ColorCashGold     = Color(0xFFF59E0B)
private val ColorCashDark     = Color(0xFF1A1300)
private val ColorCardBlue     = Color(0xFF3B82F6)
private val ColorCardDark     = Color(0xFF03091A)
private val ColorCreditPurple = Color(0xFFA855F7)
private val ColorCreditDark   = Color(0xFF0E0318)

private val ColorBrand        = Color(0xFF2563EB)
private val ColorBrandLight   = Color(0xFF60A5FA)

private val ColorBillBg       = Color(0xFFF1F4F8)
private val ColorBillCard     = Color(0xFFFFFFFF)
private val ColorBillDark     = Color(0xFF0D1520)
private val ColorBillRowAlt   = Color(0xFFF8FAFC)

/* ============================================================
   PREFS
   ============================================================ */

private const val PREFS_NAME   = "quickbill_customer_display"
private const val PREF_IP      = "desktop_ip"
private const val DEFAULT_IP   = "192.168.0.227"
private const val DEFAULT_PORT = 8765

private const val SUCCESS_LOCK_MS = 5000L

/* ============================================================
   ACTIVITY
   ============================================================ */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBars()

        setContent {
            QuickBillCustomerDisplayTheme {
                QuickBillDisplayApp(context = this)
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            hide(WindowInsetsCompat.Type.systemBars())

            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            hideSystemBars()
        }
    }
}

/* ============================================================
   ROOT APP — STATE MACHINE
   ============================================================ */

@Composable
private fun QuickBillDisplayApp(context: Context) {

    val preferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var desktopIp by remember {
        mutableStateOf(preferences.getString(PREF_IP, DEFAULT_IP) ?: DEFAULT_IP)
    }

    var connected by remember { mutableStateOf(false) }
    var bill by remember { mutableStateOf(BillState()) }
    var payment by remember { mutableStateOf(PaymentState()) }
    var screen by remember { mutableStateOf(DisplayScreen.WELCOME) }
    var showSettings by remember { mutableStateOf(false) }
    var reconnectKey by remember { mutableStateOf(0) }

    // Success lock: once SUCCESS is entered for a bill, ignore interrupting events for 5s
    var successLocked by remember { mutableStateOf(false) }
    var successBillNo by remember { mutableStateOf("") }
    var successStartMs by remember { mutableLongStateOf(0L) }

    // Pending state that arrived during success lock
    var pendingBill by remember { mutableStateOf<BillState?>(null) }
    var pendingGoWelcome by remember { mutableStateOf(false) }

    fun enterSuccess(newPayment: PaymentState, currentBill: BillState) {
        val billNo = currentBill.billNo
        // Ignore duplicate success for the same transaction
        if (successLocked && billNo.isNotEmpty() && billNo == successBillNo) {
            return
        }
        payment = newPayment
        bill = currentBill
        screen = DisplayScreen.SUCCESS
        successLocked = true
        successBillNo = billNo
        successStartMs = System.currentTimeMillis()
        pendingBill = null
        pendingGoWelcome = false
    }

    fun applyPendingAfterSuccess() {
        successLocked = false
        successBillNo = ""
        payment = PaymentState()

        val pending = pendingBill
        pendingBill = null

        if (pending != null) {
            bill = pending
            if (pending.items.isNotEmpty()) {
                screen = DisplayScreen.BILL
            } else {
                screen = DisplayScreen.WELCOME
            }
        } else if (pendingGoWelcome) {
            pendingGoWelcome = false
            bill = BillState()
            screen = DisplayScreen.WELCOME
        } else {
            // No pending events — clean idle
            bill = BillState()
            screen = DisplayScreen.WELCOME
        }
    }

    val socketManager = remember {
        WebSocketManager(

            onConnectionChanged = { isConnected ->
                connected = isConnected
            },

            onBillUpdate = { newBill ->
                if (successLocked) {
                    // Hold the update until success period ends
                    pendingBill = newBill
                    pendingGoWelcome = false
                } else {
                    bill = newBill
                    if (newBill.items.isNotEmpty()) {
                        // Immediately switch to BILL on first item
                        if (screen != DisplayScreen.PAYMENT && screen != DisplayScreen.SUCCESS) {
                            screen = DisplayScreen.BILL
                        }
                    }
                }
            },

            onPaymentUpdate = { newPayment ->
                if (successLocked) {
                    // Ignore payment updates while success is locked
                    return@WebSocketManager
                }
                payment = newPayment
                when (newPayment.status) {
                    PaymentStatus.STARTED,
                    PaymentStatus.PENDING -> {
                        screen = DisplayScreen.PAYMENT
                    }
                    PaymentStatus.COMPLETED -> {
                        enterSuccess(newPayment, bill)
                    }
                    PaymentStatus.IDLE -> {
                        if (bill.items.isNotEmpty() && screen != DisplayScreen.SUCCESS) {
                            screen = DisplayScreen.BILL
                        }
                    }
                }
            },

            onSaleCompleted = { newPayment ->
                if (successLocked) {
                    // Same transaction or race — keep success lock
                    return@WebSocketManager
                }
                enterSuccess(newPayment, bill)
            },

            onNewBill = { newBill ->
                if (successLocked) {
                    // Do NOT interrupt SUCCESS. Store for later.
                    pendingBill = newBill
                    pendingGoWelcome = true
                } else {
                    bill = newBill
                    payment = PaymentState()
                    if (newBill.items.isNotEmpty()) {
                        screen = DisplayScreen.BILL
                    } else {
                        screen = DisplayScreen.WELCOME
                    }
                }
            },

            onError = { _ ->
                // Silent — never show technical errors to customer
                connected = false
            }
        )
    }

    LaunchedEffect(Unit) {
        val savedIp = preferences
            .getString(PREF_IP, DEFAULT_IP)
            ?.trim()
            .takeUnless { it.isNullOrEmpty() }
            ?: DEFAULT_IP

        socketManager.connect(
            ipAddress = savedIp,
            port = DEFAULT_PORT
        )
    }

    DisposableEffect(Unit) {
        onDispose { socketManager.disconnect() }
    }

    /* Success lock timer — absolute 5 seconds, cannot be cancelled by new events */
    LaunchedEffect(successLocked, successStartMs) {
        if (successLocked) {
            val elapsed = System.currentTimeMillis() - successStartMs
            val remaining = (SUCCESS_LOCK_MS - elapsed).coerceAtLeast(0L)
            delay(remaining)
            applyPendingAfterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorInk)
    ) {

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(380)) + scaleIn(tween(380), initialScale = 0.97f))
                    .togetherWith(fadeOut(tween(240)))
            },
            label = "display_screen"
        ) { currentScreen ->

            when (currentScreen) {
                DisplayScreen.WELCOME -> WelcomeScreen(onSettings = { showSettings = true })
                DisplayScreen.BILL    -> BillScreen(bill = bill)
                DisplayScreen.PAYMENT -> PaymentScreen(bill = bill, payment = payment)
                DisplayScreen.SUCCESS -> SuccessScreen(bill = bill, payment = payment)
            }
        }

        if (showSettings) {
            ConnectionSettingsDialog(
                currentIp = desktopIp,
                onDismiss = { showSettings = false },
                onSave    = { newIp ->
                    val cleanIp = newIp.trim()
                    if (cleanIp.isNotEmpty()) {
                        preferences.edit().putString(PREF_IP, cleanIp).apply()
                        desktopIp = cleanIp
                        reconnectKey++
                        showSettings = false
                    }
                }
            )
        }
    }
}

/* ============================================================
   WELCOME / IDLE SCREEN
   ============================================================ */

@Composable
private fun WelcomeScreen(onSettings: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "idle_ambient")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue  = 0.14f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val subtlePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = 1.035f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorInk)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ColorBrand.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.38f),
                        radius = 980f
                    )
                )
        )

        /* Nearly invisible settings access */
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
                .size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFF1E2A38),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .scale(subtlePulse)
                    .size(128.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ColorBrand, Color(0xFF1D4ED8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QB",
                    color = ColorWhite,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "QuickBill",
                color = ColorWhite,
                fontSize = 58.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "SMART BILLING",
                color = ColorBrandLight.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 5.sp
            )

            Spacer(modifier = Modifier.height(58.dp))

            Text(
                text = "Welcome",
                color = ColorWhite,
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Your bill will appear here",
                color = ColorMuted,
                fontSize = 21.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(84.dp))

            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(1.dp)
                    .background(ColorSubtle)
            )

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Thank you for shopping with us",
                color = ColorSubtle,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

/* ============================================================
   BILL SCREEN
   ============================================================ */

@Composable
private fun BillScreen(bill: BillState) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBillBg)
    ) {

        /* Header */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorBillDark)
                .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ColorBrand),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QB",
                        color = ColorWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "QuickBill",
                    color = ColorWhite,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                if (bill.customer.name.isNotEmpty()) {
                    Text(
                        text = bill.customer.name,
                        color = ColorWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (bill.billNo.isNotEmpty()) {
                    Text(
                        text = "Bill  ${bill.billNo}",
                        color = ColorMuted,
                        fontSize = 14.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        /* Body: scrollable items + fixed total panel */
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 22.dp, bottom = 22.dp, top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ItemsCard(bill = bill, modifier = Modifier.weight(1f))
            BillTotalPanel(bill = bill)
        }
    }
}

/* ============================================================
   ITEMS TABLE + RELIABLE AUTO-SCROLL
   ============================================================ */

@Composable
private fun ItemsCard(bill: BillState, modifier: Modifier) {

    val listState = rememberLazyListState()

    // Content signature that changes on ANY meaningful update:
    // size, qty, amount, name, barcode — not only list size.
    val itemSignature = remember(bill.items) {
        bill.items.joinToString(separator = "|") { item ->
            "${item.barcode}:${item.name}:${item.qty}:${item.amount}:${item.rate}"
        }
    }

    LaunchedEffect(itemSignature) {

        if (bill.items.isNotEmpty()) {

            try {

                withFrameNanos { }

                withFrameNanos { }

                listState.animateScrollToItem(
                    index = bill.items.lastIndex
                )

            } catch (_: Exception) {
                // Ignore scroll errors.
            }
        }
    }

    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ColorBillCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            /* Column headers */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEDF1F6))
                    .padding(horizontal = 24.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ITEM",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF52637A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
                Text(
                    text = "QTY",
                    modifier = Modifier.width(70.dp),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF52637A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "RATE",
                    modifier = Modifier.width(108.dp),
                    textAlign = TextAlign.End,
                    color = Color(0xFF52637A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AMOUNT",
                    modifier = Modifier.width(126.dp),
                    textAlign = TextAlign.End,
                    color = Color(0xFF52637A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (bill.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waiting for items…",
                        color = Color(0xFFB0BBC9),
                        fontSize = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = bill.items,
                        key = { index ->
                            "bill-item-$index"
                        }
                    ) { item ->
                        BillItemRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillItemRow(item: BillItem) {

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 18.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = item.name,
                    color = Color(0xFF0D1520),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.brand.isNotEmpty()) {

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = item.brand,
                        color = Color(0xFFAAB8C8),
                        fontSize = 13.sp
                    )
                }
            }

            Text(
                text = item.qty.toString(),
                modifier = Modifier.width(70.dp),
                textAlign = TextAlign.Center,
                color = Color(0xFF243447),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "₹${money(item.rate)}",
                modifier = Modifier.width(108.dp),
                textAlign = TextAlign.End,
                color = Color(0xFF52637A),
                fontSize = 20.sp
            )

            Text(
                text = "₹${money(item.amount)}",
                modifier = Modifier.width(126.dp),
                textAlign = TextAlign.End,
                color = Color(0xFF0D1520),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE8EDF3))
        )
    }
}

/* ============================================================
   TOTAL PANEL — always visible, never scrolls
   ============================================================ */

@Composable
private fun BillTotalPanel(bill: BillState) {

    val totalScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "total_scale"
    )

    Card(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ColorBillDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp)
        ) {

            Text(
                text = "SUMMARY",
                color = Color(0xFF4A5A6E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            SummaryRow(label = "Subtotal", value = "₹${money(bill.subtotal)}")
            SummaryRow(label = "Tax", value = "+ ₹${money(bill.tax)}")
            SummaryRow(label = "Discount", value = "- ₹${money(bill.discount)}")

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF1F2D3E))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TOTAL",
                color = Color(0xFF8098B4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "₹${money(bill.total)}",
                color = ColorWhite,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.scale(totalScale)
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Payment screen will follow",
                color = Color(0xFF2E3F52),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF6A7F96), fontSize = 16.sp)
        Text(
            text = value,
            color = Color(0xFFB8C8D8),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ============================================================
   PAYMENT SCREEN
   ============================================================ */

@Composable
private fun PaymentScreen(bill: BillState, payment: PaymentState) {

    val mode = payment.mode

    val accentColor = when (mode) {
        PaymentMode.UPI    -> ColorUpiGreen
        PaymentMode.CASH   -> ColorCashGold
        PaymentMode.CARD   -> ColorCardBlue
        PaymentMode.CREDIT -> ColorCreditPurple
        else               -> ColorBrand
    }

    val darkBg = when (mode) {
        PaymentMode.UPI    -> ColorUpiDark
        PaymentMode.CASH   -> ColorCashDark
        PaymentMode.CARD   -> ColorCardDark
        PaymentMode.CREDIT -> ColorCreditDark
        else               -> ColorInk
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pay_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.07f,
        targetValue = 0.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pay_glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.32f),
                        radius = 820f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QuickBill",
                    color = ColorWhite.copy(alpha = 0.45f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (bill.billNo.isNotEmpty()) {
                    Text(
                        text = bill.billNo,
                        color = ColorSubtle,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            PaymentModeIcon(mode = mode, accentColor = accentColor)

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = paymentTitle(mode).uppercase(),
                color = ColorWhite,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = paymentSubtitle(mode, payment.status),
                color = accentColor.copy(alpha = 0.9f),
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "₹${money(bill.total)}",
                color = ColorWhite,
                fontSize = 70.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            when (mode) {
                PaymentMode.UPI -> UpiPaymentPanel(
                    payment = payment,
                    accentColor = accentColor
                )
                PaymentMode.CASH -> InstructionPanel(
                    icon = Icons.Default.Payment,
                    message = "Please pay the cashier",
                    accent = accentColor
                )
                PaymentMode.CARD -> InstructionPanel(
                    icon = Icons.Default.CreditCard,
                    message = "Please use the card terminal",
                    accent = accentColor
                )
                PaymentMode.CREDIT -> InstructionPanel(
                    icon = Icons.Default.Payment,
                    message = "Processing your credit sale…",
                    accent = accentColor
                )
                else -> Unit
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PaymentModeIcon(mode: PaymentMode, accentColor: Color) {
    val icon = when (mode) {
        PaymentMode.UPI    -> Icons.Default.QrCode2
        PaymentMode.CARD   -> Icons.Default.CreditCard
        PaymentMode.CASH   -> Icons.Default.Payment
        PaymentMode.CREDIT -> Icons.Default.Payment
        else               -> Icons.Default.Payment
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.14f))
            .border(2.dp, accentColor.copy(alpha = 0.38f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
private fun UpiPaymentPanel(payment: PaymentState, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(ColorWhite),
            contentAlignment = Alignment.Center
        ) {
            if (payment.qrEnabled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(136.dp)
                    )
                    Text(
                        text = "UPI",
                        color = Color(0xFF1A1A1A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "QR",
                    color = Color(0xFF888888),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SCAN TO PAY",
            color = accentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Use any UPI app",
            color = ColorMuted,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun InstructionPanel(
    icon: ImageVector,
    message: String,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.62f),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = ColorWhite.copy(alpha = 0.07f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(50.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = ColorWhite,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ============================================================
   SUCCESS SCREEN — premium, locked for 5 seconds
   ============================================================ */

@Composable
private fun SuccessScreen(bill: BillState, payment: PaymentState) {

    var checkVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        checkVisible = true
    }

    val checkScale by animateFloatAsState(
        targetValue = if (checkVisible) 1.0f else 0.35f,
        animationSpec = tween(560, easing = FastOutSlowInEasing),
        label = "check_scale"
    )

    val checkAlpha by animateFloatAsState(
        targetValue = if (checkVisible) 1.0f else 0.0f,
        animationSpec = tween(400),
        label = "check_alpha"
    )

    val modeLabel = when (payment.mode) {
        PaymentMode.UPI    -> "UPI"
        PaymentMode.CASH   -> "Cash"
        PaymentMode.CARD   -> "Card"
        PaymentMode.CREDIT -> "Credit"
        else               -> payment.mode.name
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorSuccessDark),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ColorSuccess.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.36f),
                        radius = 780f
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {

            Box(
                modifier = Modifier
                    .scale(checkScale)
                    .alpha(checkAlpha)
                    .size(168.dp)
                    .clip(CircleShape)
                    .background(ColorSuccess),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ColorWhite,
                    modifier = Modifier.size(104.dp)
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            Text(
                text = "PAYMENT SUCCESSFUL",
                color = ColorWhite,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "₹${money(bill.total)}",
                color = ColorSuccess,
                fontSize = 66.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Paid via $modeLabel",
                color = Color(0xFF5DBD85),
                fontSize = 23.sp
            )

            if (bill.billNo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = bill.billNo,
                    color = Color(0xFF2E5C40),
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(46.dp))

            Text(
                text = "Thank you for shopping with us",
                color = Color(0xFF7DCBA0),
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Please visit again",
                color = ColorWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* ============================================================
   CONNECTION SETTINGS DIALOG
   ============================================================ */

@Composable
private fun ConnectionSettingsDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var ip by remember { mutableStateOf(currentIp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Desktop Connection", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Enter the IPv4 address of the computer running QuickBill Desktop.",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Desktop IP address") },
                    singleLine = true,
                    placeholder = { Text("192.168.0.227") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Port: $DEFAULT_PORT", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Example: 192.168.0.227", color = Color.Gray, fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ip) },
                enabled = ip.trim().isNotEmpty()
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/* ============================================================
   HELPERS
   ============================================================ */

private fun paymentTitle(mode: PaymentMode): String = when (mode) {
    PaymentMode.UPI    -> "UPI Payment"
    PaymentMode.CASH   -> "Cash Payment"
    PaymentMode.CARD   -> "Card Payment"
    PaymentMode.CREDIT -> "Credit Sale"
    else               -> "Payment"
}

private fun paymentSubtitle(mode: PaymentMode, status: PaymentStatus): String = when (mode) {
    PaymentMode.UPI -> when (status) {
        PaymentStatus.STARTED -> "Please scan the QR code"
        PaymentStatus.PENDING -> "Waiting for payment confirmation…"
        else                  -> "Complete your UPI payment"
    }
    PaymentMode.CASH   -> "Please pay the cashier"
    PaymentMode.CARD   -> "Please complete payment on the terminal"
    PaymentMode.CREDIT -> "Processing credit sale…"
    else               -> "Processing payment"
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)