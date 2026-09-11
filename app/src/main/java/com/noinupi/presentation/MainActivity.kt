package com.noinupi.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.offset
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.draw.clipToBounds
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.noinupi.app.R
import com.noinupi.domain.ActionEvent
import com.noinupi.domain.ActionRunner
import com.noinupi.domain.Actions
import com.noinupi.domain.Transaction
import com.noinupi.domain.TransactionStore
import com.noinupi.platform.UssdEngine

import kotlinx.coroutines.launch


// ============================================================
// TERMINAL COLORS
// ============================================================

val TerminalBg = Color(0xFF000000)
val TerminalPanel = Color(0xFF09120F)
val TerminalAmber = Color(0xFFD5A84A)
val TerminalBright = Color(0xFFE7C96B)
val TerminalMuted = Color(0xFF8C743C)
val TerminalDark = Color(0xFF111111)


// ============================================================
// 8-BIT FONT
// ============================================================

val PixelFont = FontFamily(
    Font(R.font.press_start_2p)
)


// ============================================================
// MAIN ACTIVITY
// ============================================================

class MainActivity : ComponentActivity() {

    private lateinit var engine: UssdEngine
    private lateinit var actionRunner: ActionRunner
    private lateinit var transactionStore: TransactionStore

    private var transactions by mutableStateOf(
        emptyList<Transaction>()
    )

    private var currentBalance by mutableStateOf<String?>(null)

    private var scannedUpi by mutableStateOf("")
    private var scannedAmount by mutableStateOf("")
    private var paymentResetKey by mutableStateOf(0)

    private val prefs by lazy {
        getSharedPreferences(
            "noinupi_prefs",
            MODE_PRIVATE
        )
    }

    private val callPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startPayment()
            } else {
                Toast.makeText(
                    this,
                    "Phone permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        transactionStore = TransactionStore(this)
        transactions = transactionStore.getAll()

        engine = UssdEngine(this)
        actionRunner = ActionRunner(engine)

        currentBalance = prefs.getString("balance", null)

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TerminalBg
                ) {

                    var page by remember {
                        mutableStateOf("home")
                    }

                    when (page) {

                        "home" -> {

                            PayScreen(
                                scannedUpi = scannedUpi,
                                scannedAmount = scannedAmount,
                                paymentResetKey = paymentResetKey,

                                savedBalance = currentBalance,

                                savedReceiveUpi = prefs.getString(
                                    "receive_upi",
                                    ""
                                ) ?: "",
                                balanceTime = prefs.getLong(
                                    "balance_time",
                                    0L
                                ),

                                onPay = { upi, amount ->

                                    UssdSessionStore.vpa = upi
                                    UssdSessionStore.amount = amount

                                    if (
                                        ContextCompat.checkSelfPermission(
                                            this@MainActivity,
                                            Manifest.permission.CALL_PHONE
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {

                                        callPermission.launch(
                                            Manifest.permission.CALL_PHONE
                                        )

                                    } else {

                                        startPayment()
                                    }
                                },

                                onCheckBalance = {
                                    checkBalance()
                                },

                                onSaveReceiveUpi = { upi ->
                                    prefs.edit()
                                        .putString(
                                            "receive_upi",
                                            upi
                                        )
                                        .apply()
                                },

                                onOpenHistory = {
                                    page = "history"
                                },

                                onOpenQr = {
                                    page = "qr"
                                },

                                transactions = transactions
                            )
                        }

                        "qr" -> {

                            QrMenuScreen(
                                onSend = {
                                    page = "scanner"
                                },
                                onReceive = {
                                    page = "receive"
                                },
                                onBack = {
                                    page = "home"
                                }
                            )
                        }

                        "receive" -> {

                            ReceiveScreen(
                                upiId = prefs.getString(
                                    "receive_upi",
                                    ""
                                ) ?: "",

                                onSave = { upi ->
                                    prefs.edit()
                                        .putString(
                                            "receive_upi",
                                            upi
                                        )
                                        .apply()
                                },

                                onBack = {
                                    page = "qr"
                                }
                            )
                        }

                        "scanner" -> {

                            ScannerScreen(
                                onResult = { upi, amount ->

                                    scannedUpi = upi
                                    scannedAmount = amount

                                    UssdSessionStore.vpa = upi
                                    UssdSessionStore.amount = amount

                                    page = "home"
                                },

                                onBack = {
                                    page = "qr"
                                }
                            )
                        }

                        "history" -> {

                            HistoryScreen(
                                transactions = transactions,
                                onBack = {
                                    page = "home"
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    // ============================================================
    // PAYMENT
    // ============================================================

    private fun startPayment() {

        lifecycleScope.launch {

            if (!actionRunner.isServiceEnabled()) {

                Toast.makeText(
                    this@MainActivity,
                    "Turn on NoInUPI Accessibility first",
                    Toast.LENGTH_LONG
                ).show()

                return@launch
            }

            val run = actionRunner.runAction(
                action = Actions.SendUpi,
                vars = mapOf(
                    "vpa" to UssdSessionStore.vpa,
                    "amount" to UssdSessionStore.amount,
                    "note" to "",
                    "pin" to ""
                ),
                scope = lifecycleScope
            )

            launch {

                run.events.collect { event ->

                    when (event) {

                        is ActionEvent.Progress -> {

                            if (
                                event.label?.contains(
                                    "UPI PIN",
                                    true
                                ) == true
                            ) {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Enter your UPI PIN in the carrier dialog",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        is ActionEvent.Error -> {

                            Toast.makeText(
                                this@MainActivity,
                                event.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is ActionEvent.Done -> {

                            transactionStore.save(
                                Transaction(
                                    upiId = UssdSessionStore.vpa,
                                    amount = UssdSessionStore.amount,
                                    timestamp = System.currentTimeMillis(),
                                    status = "Success"
                                )
                            )

                            transactions = transactionStore.getAll()

                            UssdSessionStore.vpa = ""
                            UssdSessionStore.amount = ""

                            scannedUpi = ""
                            scannedAmount = ""
                            paymentResetKey++



                            Toast.makeText(
                                this@MainActivity,
                                "Payment complete",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    // ============================================================
    // CHECK BALANCE
    // ============================================================

    private fun checkBalance() {

        lifecycleScope.launch {

            if (!actionRunner.isServiceEnabled()) {

                Toast.makeText(
                    this@MainActivity,
                    "Turn on NoInUPI Accessibility first",
                    Toast.LENGTH_LONG
                ).show()

                return@launch
            }

            val run = actionRunner.runAction(
                action = Actions.CheckBalance,
                vars = emptyMap(),
                scope = lifecycleScope
            )

            launch {

                run.events.collect { event ->

                    when (event) {

                        is ActionEvent.Progress -> {

                            if (
                                event.label?.contains(
                                    "UPI PIN",
                                    true
                                ) == true
                            ) {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Enter your UPI PIN in the carrier dialog",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        is ActionEvent.Error -> {

                            Toast.makeText(
                                this@MainActivity,
                                event.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is ActionEvent.Done -> {

                            val balance = extractBalance(
                                event.resultText ?: ""
                            )

                            if (balance != null) {

                                prefs.edit()
                                    .putString(
                                        "balance",
                                        balance
                                    )
                                    .putLong(
                                        "balance_time",
                                        System.currentTimeMillis()
                                    )
                                    .apply()

                                currentBalance = balance

                                Toast.makeText(
                                    this@MainActivity,
                                    "Balance updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    // ============================================================
    // BALANCE EXTRACTION
    // ============================================================

    private fun extractBalance(text: String): String? {

        val patterns = listOf(

            Regex(
                """(?:available\s+balance|balance)\s*[:\-]?\s*(?:rs\.?|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
                RegexOption.IGNORE_CASE
            ),

            Regex(
                """(?:rs\.?|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""",
                RegexOption.IGNORE_CASE
            )
        )

        for (pattern in patterns) {

            val match = pattern.find(text)

            if (match != null) {

                return match
                    .groupValues[1]
                    .replace(",", "")
            }
        }

        return null
    }
}


// ============================================================
// SESSION STORE
// ============================================================

object UssdSessionStore {

    var vpa: String = ""
    var amount: String = ""
}


// ============================================================
// PAY SCREEN
// ============================================================

@Composable
fun PayScreen(
    scannedUpi: String,
    scannedAmount: String,
    paymentResetKey: Int,
    savedBalance: String?,
    savedReceiveUpi: String,
    balanceTime: Long,
    onPay: (String, String) -> Unit,
    onCheckBalance: () -> Unit,
    onSaveReceiveUpi: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenQr: () -> Unit,
    transactions: List<Transaction>
) {

    var upiId by remember(paymentResetKey, scannedUpi) {
        mutableStateOf(scannedUpi)
    }

    var amount by remember(paymentResetKey, scannedUpi, scannedAmount) {
        mutableStateOf(scannedAmount)
    }

    val lastChecked =
        if (balanceTime == 0L) {
            "NEVER"
        } else {
            java.text.SimpleDateFormat(
                "dd/MM/yyyy  HH:mm",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(balanceTime)
            )
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "NOINUPI",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 16.sp
            )

            IconButton(
                onClick = onOpenQr
            ) {

                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "QR",
                    tint = TerminalAmber,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "UPI ID",
            color = TerminalMuted,
            fontFamily = PixelFont,
            fontSize = 7.sp
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        OutlinedTextField(
            value = upiId,
            onValueChange = {
                upiId = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TerminalBright,
                fontFamily = PixelFont,
                fontSize = 9.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerminalAmber,
                unfocusedBorderColor = TerminalMuted,
                cursorColor = TerminalAmber,
                focusedTextColor = TerminalBright,
                unfocusedTextColor = TerminalBright
            )
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "AMOUNT",
            color = TerminalMuted,
            fontFamily = PixelFont,
            fontSize = 7.sp
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TerminalBright,
                fontFamily = PixelFont,
                fontSize = 9.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerminalAmber,
                unfocusedBorderColor = TerminalMuted,
                cursorColor = TerminalAmber,
                focusedTextColor = TerminalBright,
                unfocusedTextColor = TerminalBright
            )
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        Button(
            onClick = {

                if (
                    upiId.isNotBlank() &&
                    amount.isNotBlank()
                ) {

                    onPay(
                        upiId.trim(),
                        amount.trim()
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TerminalAmber,
                contentColor = Color.Black
            )
        ) {

            Text(
                text = "ENTER",
                fontFamily = PixelFont,
                fontSize = 8.sp
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "AVAILABLE BALANCE",
            color = TerminalMuted,
            fontFamily = PixelFont,
            fontSize = 7.sp
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )


        // ========================================================
        // BALANCE DISPLAY IMAGE
        // ========================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {

            // Exact balance display image.
            Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = R.drawable.balance_display
                ),
                contentDescription = "Balance display",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Dynamic balance + last checked text.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 24.dp,
                        top = 60.dp,
                        end = 24.dp,
                        bottom = 0.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(
                    modifier = Modifier.height(55.dp)
                )

                Text(
                    text = if (savedBalance != null) {
                        "₹$savedBalance"
                    } else {
                        "NO DATA"
                    },
                    color = TerminalBright,
                    fontFamily = PixelFont,
                    fontSize = 17.sp,
                    modifier = Modifier.offset(y = 20.dp)
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "LAST CHECKED : $lastChecked",
                    color = TerminalMuted,
                    fontFamily = PixelFont,
                    fontSize = 6.sp
                )
            }


            // ====================================================
            // SUBTLE CRT SCANLINES
            // ====================================================

            var scanOffset by remember {
                mutableStateOf(0f)
            }

            LaunchedEffect(Unit) {

                while (true) {

                    scanOffset += 1f

                    if (scanOffset >= 8f) {
                        scanOffset = 0f
                    }

                    kotlinx.coroutines.delay(45L)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {

                val spacing = 8f
                var y = scanOffset

                while (y < size.height) {

                    drawLine(
                        color = Color.White.copy(
                            alpha = 0.055f
                        ),
                        start = Offset(
                            0f,
                            y
                        ),
                        end = Offset(
                            size.width,
                            y
                        ),
                        strokeWidth = 5f
                    )

                    y += spacing
                }
            }
        }


        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = onCheckBalance,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                1.dp,
                TerminalAmber
            )
        ) {

            Text(
                text = "CHECK AGAIN",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 7.sp
            )
        }

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(
                1.dp,
                TerminalAmber
            )
        ) {

            Text(
                text = "TRANSACTION LOG  [${transactions.size}]",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 7.sp
            )
        }
    }
}


// ============================================================
// QR MENU
// ============================================================

@Composable
fun QrMenuScreen(
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(18.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TerminalAmber
                )
            }

            Text(
                text = "QR TERMINAL",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 11.sp
            )
        }

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        TerminalButton(
            text = "SEND",
            onClick = onSend
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        TerminalButton(
            text = "RECEIVE",
            onClick = onReceive
        )
    }
}


// ============================================================
// RECEIVE
// ============================================================

@Composable
fun ReceiveScreen(
    upiId: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {

    var value by remember {
        mutableStateOf(upiId)
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TerminalAmber
                )
            }

            Text(
                text = "RECEIVE",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 11.sp
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Text(
            text = "YOUR UPI ID",
            color = TerminalMuted,
            fontFamily = PixelFont,
            fontSize = 7.sp
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TerminalBright,
                fontFamily = PixelFont,
                fontSize = 8.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TerminalAmber,
                unfocusedBorderColor = TerminalMuted,
                cursorColor = TerminalAmber,
                focusedTextColor = TerminalBright,
                unfocusedTextColor = TerminalBright
            )
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        TerminalButton(
            text = "SAVE",
            onClick = {
                onSave(value.trim())
            }
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        if (value.isNotBlank()) {

            val bitmap = remember(value) {
                generateQrBitmap(
                    "upi://pay?pa=${Uri.encode(value)}&cu=INR"
                )
            }

            if (bitmap != null) {

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "UPI QR",
                    modifier = Modifier
                        .size(230.dp)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


// ============================================================
// SCANNER
// ============================================================

@Composable
fun ScannerScreen(
    onResult: (String, String) -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var showCamera by remember {
        mutableStateOf(false)
    }

    val launcher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                scanQrFromImage(
                    context,
                    uri
                ) { upi, amount ->

                    onResult(
                        upi,
                        amount
                    )
                }
            }
        }

    BackHandler {
        onBack()
    }

    if (showCamera) {

        CameraScannerScreen(
            onResult = { upi, amount ->

                showCamera = false

                onResult(
                    upi,
                    amount
                )
            },
            onBack = {
                showCamera = false
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .padding(18.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TerminalAmber
                )
            }

            Text(
                text = "SCAN / UPLOAD",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 10.sp
            )
        }

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        TerminalButton(
            text = "SCAN QR",
            onClick = {
                showCamera = true
            }
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        TerminalButton(
            text = "UPLOAD QR",
            onClick = {
                launcher.launch("image/*")
            }
        )
    }
}


// ============================================================
// CAMERA SCANNER
// ============================================================

@Composable
fun CameraScannerScreen(
    onResult: (String, String) -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            hasPermission = granted
        }

    LaunchedEffect(Unit) {

        if (!hasPermission) {

            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    BackHandler {
        onBack()
    }

    if (!hasPermission) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TerminalBg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "CAMERA PERMISSION REQUIRED",
                color = TerminalAmber,
                fontFamily = PixelFont,
                fontSize = 8.sp
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            TerminalButton(
                text = "ALLOW CAMERA",
                onClick = {
                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                }
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            TerminalButton(
                text = "BACK",
                onClick = onBack
            )
        }

        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),

        factory = { ctx ->

            val previewView =
                androidx.camera.view.PreviewView(ctx)

            val cameraProviderFuture =
                androidx.camera.lifecycle.ProcessCameraProvider
                    .getInstance(ctx)

            cameraProviderFuture.addListener({

                val cameraProvider =
                    cameraProviderFuture.get()

                val preview =
                    androidx.camera.core.Preview
                        .Builder()
                        .build()

                preview.setSurfaceProvider(
                    previewView.surfaceProvider
                )

                val scanner =
                    BarcodeScanning.getClient()

                val analysis =
                    androidx.camera.core.ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(
                            androidx.camera.core.ImageAnalysis
                                .STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()

                analysis.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx)
                ) { imageProxy ->

                    val mediaImage =
                        imageProxy.image

                    if (mediaImage != null) {

                        val image =
                            InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->

                                val raw =
                                    barcodes
                                        .firstOrNull()
                                        ?.rawValue

                                if (!raw.isNullOrBlank()) {

                                    val parsed = parseUpiQr(raw)

                                    if (parsed != null) {

                                        cameraProvider.unbindAll()

                                        onResult(
                                            parsed.first,
                                            parsed.second
                                        )
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }

                    } else {

                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}


// ============================================================
// TERMINAL BUTTON
// ============================================================

@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            1.dp,
            TerminalAmber
        )
    ) {

        Text(
            text = text,
            color = TerminalAmber,
            fontFamily = PixelFont,
            fontSize = 8.sp
        )
    }
}


// ============================================================
// QR GENERATOR
// ============================================================

fun generateQrBitmap(text: String): Bitmap? {
    return try {
        val size = 600

        val matrix =
            com.google.zxing.qrcode.QRCodeWriter()
                .encode(
                    text,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    size,
                    size
                )

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix[x, y]) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                )
            }
        }

        bitmap

    } catch (e: Exception) {
        null
    }
}


// ============================================================
// QR IMAGE SCANNER
// ============================================================

fun scanQrFromImage(
    context: android.content.Context,
    uri: Uri,
    onResult: (String, String) -> Unit
) {

    try {

        val image =
            InputImage.fromFilePath(
                context,
                uri
            )

        BarcodeScanning
            .getClient()
            .process(image)
            .addOnSuccessListener { barcodes ->

                val value =
                    barcodes
                        .firstOrNull()
                        ?.rawValue

                if (!value.isNullOrBlank()) {

                    val parsed = parseUpiQr(value)

                    if (parsed != null) {

                        onResult(
                            parsed.first,
                            parsed.second
                        )

                    } else {

                        Toast.makeText(
                            context,
                            "Invalid UPI QR code",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    Toast.makeText(
                        context,
                        "No QR code found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    context,
                    "Unable to scan QR",
                    Toast.LENGTH_SHORT
                ).show()
            }

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "Unable to open image",
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun parseUpiQr(
    raw: String
): Pair<String, String>? {

    return try {
        val uri = Uri.parse(raw)

        if (!uri.scheme.equals("upi", ignoreCase = true)) {
            return null
        }

        val upiId = uri.getQueryParameter("pa")

        if (upiId.isNullOrBlank()) {
            return null
        }

        val amount = uri.getQueryParameter("am") ?: ""

        Pair(
            upiId,
            amount
        )

    } catch (e: Exception) {
        null
    }
}