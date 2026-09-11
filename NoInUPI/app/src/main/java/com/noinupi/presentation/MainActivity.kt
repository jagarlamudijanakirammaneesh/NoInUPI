package com.noinupi.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.noinupi.domain.ActionEvent
import com.noinupi.domain.ActionRunner
import com.noinupi.domain.Actions
import com.noinupi.platform.UssdEngine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.CameraSelector
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import com.noinupi.domain.Transaction
import com.noinupi.domain.TransactionStore

class MainActivity : ComponentActivity() {

    private lateinit var engine: UssdEngine
    private lateinit var actionRunner: ActionRunner

    private lateinit var transactionStore: TransactionStore

    private var transactions by mutableStateOf(emptyList<Transaction>())

    private val prefs by lazy {
        getSharedPreferences("noinupi_prefs", MODE_PRIVATE)
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


        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = TerminalBg) {
                    var page by remember { mutableStateOf("home") }

                    when (page) {
                        "home" -> PayScreen(
                            savedBalance = prefs.getString("balance", null),
                            savedReceiveUpi = prefs.getString("receive_upi", "") ?: "",
                            balanceTime = prefs.getLong("balance_time", 0L),
                            onPay = { upi, amount ->
                                UssdSessionStore.vpa = upi
                                UssdSessionStore.amount = amount
                                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                    callPermission.launch(Manifest.permission.CALL_PHONE)
                                } else startPayment()
                            },
                            onCheckBalance = { checkBalance() },
                            onSaveReceiveUpi = { upi -> prefs.edit().putString("receive_upi", upi).apply() },
                            onOpenHistory = { page = "history" },
                            onOpenQr = { page = "qr" },
                            transactions = transactions
                        )
                        "qr" -> QrMenuScreen(
                            onSend = { page = "scanner" },
                            onReceive = { page = "receive" },
                            onBack = { page = "home" }
                        )
                        "receive" -> ReceiveScreen(
                            upiId = prefs.getString("receive_upi", "") ?: "",
                            onSave = { upi -> prefs.edit().putString("receive_upi", upi).apply() },
                            onBack = { page = "qr" }
                        )
                        "scanner" -> ScannerScreen(
                            onResult = { upi, amount ->
                                UssdSessionStore.vpa = upi
                                UssdSessionStore.amount = amount ?: ""
                                page = "home"
                            },
                            onBack = { page = "qr" }
                        )
                        "history" -> HistoryScreen(
                            transactions = transactions,
                            onBack = { page = "home" }
                        )
                    }
                }
            }
        }

    }

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
                            lifecycleScope.launch {

                                val result = run.result.await()
                                val balance =
                                    extractBalance(result.resultText)

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

                                    recreate()

                                } else {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "Could not read balance",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun extractBalance(text: String): String? {

        val regexes = listOf(

            Regex(
                "(?:balance|available|avail).*?(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)",
                RegexOption.IGNORE_CASE
            ),

            Regex(
                "(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)",
                RegexOption.IGNORE_CASE
            )
        )

        for (regex in regexes) {
            val match = regex.find(text)

            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }
}

private object UssdSessionStore {

    @Volatile
    var vpa: String = ""

    @Volatile
    var amount: String = ""
}

private val TerminalBg = Color(0xFF050908)
private val TerminalPanel = Color(0xFF09120F)
private val TerminalAmber = Color(0xFFD5A84A)
private val TerminalBright = Color(0xFFE7C96B)
private val TerminalMuted = Color(0xFF8C743C)
private val TerminalDark = Color(0xFF111111)

@Composable
private fun terminalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TerminalAmber,
    unfocusedBorderColor = TerminalMuted,
    focusedLabelColor = TerminalAmber,
    unfocusedLabelColor = TerminalMuted,
    cursorColor = TerminalAmber,
    focusedTextColor = TerminalBright,
    unfocusedTextColor = TerminalBright
)

@Composable
private fun TerminalLabel(text: String) {
    Text(text, color = TerminalMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.5.sp)
}

@Composable
private fun TerminalPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(TerminalBg).padding(18.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = TerminalAmber) }
            Text(title, color = TerminalAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
private fun TerminalButton(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(70.dp),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, TerminalAmber),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalAmber)
    ) {
        if (icon != null) { Icon(icon, null, tint = TerminalAmber); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 2.sp)
            Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TerminalMuted, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun PayScreen(
    savedBalance: String?,
    balanceTime: Long,
    savedReceiveUpi: String,
    onPay: (String, String) -> Unit,
    onCheckBalance: () -> Unit,
    onSaveReceiveUpi: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenQr: () -> Unit,
    transactions: List<Transaction>
)
{
    var upiId by remember { mutableStateOf(UssdSessionStore.vpa) }
    var amount by remember { mutableStateOf(UssdSessionStore.amount) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07100E))
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "NoInUPI",
                    color = Color(0xFFD4A84A),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "NO INTERNET. JUST UPI.",
                    color = Color(0xFF9D7B35),
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp
                )
            }

            IconButton(onClick = onOpenQr) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "QR",
                    tint = Color(0xFFD4A84A)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("UPI ID") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD4A84A),
                unfocusedBorderColor = Color(0xFF6F5A2D),
                focusedLabelColor = Color(0xFFD4A84A),
                unfocusedLabelColor = Color(0xFF9D7B35),
                cursorColor = Color(0xFFD4A84A),
                focusedTextColor = Color(0xFFE4C46A),
                unfocusedTextColor = Color(0xFFE4C46A)
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("AMOUNT (₹)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD4A84A),
                unfocusedBorderColor = Color(0xFF6F5A2D),
                focusedLabelColor = Color(0xFFD4A84A),
                unfocusedLabelColor = Color(0xFF9D7B35),
                cursorColor = Color(0xFFD4A84A),
                focusedTextColor = Color(0xFFE4C46A),
                unfocusedTextColor = Color(0xFFE4C46A)
            )
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (upiId.isNotBlank() && amount.isNotBlank()) {
                    onPay(upiId.trim(), amount.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD4A84A),
                contentColor = Color(0xFF07100E)
            ),
            shape = RoundedCornerShape(2.dp)
        ) {
            Text(
                "ENTER",
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        // Only the balance display gets the CRT/fisheye treatment.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF030807))
                .border(2.dp, Color(0xFF6F5A2D), RoundedCornerShape(28.dp))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val lineGap = 7.dp.toPx()
                var y = 4.dp.toPx()
                while (y < size.height) {
                    drawLine(
                        color = Color(0xFF6F5A2D).copy(alpha = 0.12f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += lineGap
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "AVAILABLE BALANCE",
                    color = Color(0xFFD4A84A),
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    savedBalance ?: "--------",
                    color = Color(0xFFE4C46A),
                    fontSize = 35.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "Last checked : $balanceTime",
            color = Color(0xFF9D7B35),
            fontSize = 11.sp
        )

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCheckBalance,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, Color(0xFF6F5A2D)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD4A84A)
            )
        ) {
            Text("CHECK AGAIN", letterSpacing = 1.5.sp)
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, Color(0xFF6F5A2D)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD4A84A)
            )
        ) {
            Text(
                "TRANSACTION LOG",
                modifier = Modifier.weight(1f),
                letterSpacing = 1.5.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "OFFLINE MODE  •  *99# USSD",
            color = Color(0xFF6F5A2D),
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun QrMenuScreen(
    onSend: () -> Unit,
    onReceive: () -> Unit,
    onBack: () -> Unit
) {
    TerminalPage(title = "QR TERMINAL", onBack = onBack) {
        TerminalButton("SEND", "SCAN OR UPLOAD A UPI QR", Icons.Default.CameraAlt, onSend)
        Spacer(Modifier.height(14.dp))
        TerminalButton("RECEIVE", "SHOW YOUR UPI QR CODE", Icons.Default.QrCode2, onReceive)
    }
}

@Composable
private fun ReceiveScreen(
    upiId: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var receiveUpi by remember { mutableStateOf(upiId) }
    val qrBitmap = remember(receiveUpi) { generateUpiQr(receiveUpi) }

    TerminalPage(title = "RECEIVE", onBack = onBack) {
        TerminalLabel("YOUR UPI ID")
        OutlinedTextField(
            value = receiveUpi,
            onValueChange = { receiveUpi = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = terminalFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        TerminalButton("SAVE ID", "STORE THIS UPI ID", null, { onSave(receiveUpi.trim()) })
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.White)
                .border(3.dp, TerminalAmber)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) Image(qrBitmap.asImageBitmap(), "Your UPI QR")
            else Text("ENTER UPI ID", color = TerminalDark, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("SHOW THIS QR TO RECEIVE MONEY", color = TerminalAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp)
    }
}

private fun generateUpiQr(upiId: String): Bitmap? {
    if (upiId.isBlank()) return null

    val data = "upi://pay?pa=${Uri.encode(upiId)}&cu=INR"

    val size = 600

    val bitMatrix = com.google.zxing.qrcode.QRCodeWriter()
        .encode(
            data,
            com.google.zxing.BarcodeFormat.QR_CODE,
            size,
            size
        )

    val bitmap = Bitmap.createBitmap(
        size,
        size,
        Bitmap.Config.ARGB_8888
    )

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(
                x,
                y,
                if (bitMatrix[x, y]) Color.Black.toArgb() else Color.White.toArgb()
            )
        }
    }

    return bitmap
}

private fun timeAgo(timestamp: Long): String {

    val elapsed =
        System.currentTimeMillis() - timestamp

    val minutes =
        TimeUnit.MILLISECONDS.toMinutes(elapsed)

    return when {

        minutes < 1 ->
            "just now"

        minutes < 60 ->
            "$minutes min ago"

        minutes < 1440 ->
            "${minutes / 60} hr ago"

        else ->
            "${minutes / 1440} days ago"
    }
}

@Composable
private fun ScannerScreen(
    onResult: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var cameraMode by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val image = InputImage.fromFilePath(context, uri)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstOrNull()?.rawValue
                    if (value == null) {
                        Toast.makeText(context, "NO QR CODE FOUND", Toast.LENGTH_SHORT).show(); return@addOnSuccessListener
                    }
                    try {
                        val qrUri = Uri.parse(value)
                        if (qrUri.scheme != "upi") {
                            Toast.makeText(context, "NOT A UPI QR", Toast.LENGTH_SHORT).show(); return@addOnSuccessListener
                        }
                        val pa = qrUri.getQueryParameter("pa")
                        val am = qrUri.getQueryParameter("am")
                        if (!pa.isNullOrBlank()) onResult(pa, am)
                        else Toast.makeText(context, "INVALID UPI QR", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) { Toast.makeText(context, "INVALID QR DATA", Toast.LENGTH_SHORT).show() }
                }
                .addOnFailureListener { Toast.makeText(context, "COULD NOT READ QR", Toast.LENGTH_SHORT).show() }
        } catch (_: Exception) { Toast.makeText(context, "COULD NOT OPEN IMAGE", Toast.LENGTH_SHORT).show() }
    }

    if (cameraMode) {
        CameraScanner(onResult = onResult, onBack = { cameraMode = false })
        return
    }

    TerminalPage(title = "SEND MONEY", onBack = onBack) {
        Text("SELECT QR INPUT", color = TerminalAmber, fontFamily = FontFamily.Monospace, fontSize = 13.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(18.dp))
        TerminalButton("SCAN QR", "USE CAMERA TO READ UPI QR", Icons.Default.CameraAlt) { cameraMode = true }
        Spacer(Modifier.height(14.dp))
        TerminalButton("UPLOAD QR", "READ A QR IMAGE FROM STORAGE", Icons.Default.UploadFile) { imagePicker.launch("image/*") }
        Spacer(Modifier.height(18.dp))
        Text("QR DATA WILL FILL THE PAYMENT FORM", color = TerminalMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
private fun CameraScanner(
    onResult: (String, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onBack()
    }

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
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text("Camera permission required")

            Spacer(modifier = Modifier.padding(16.dp))

            Button(onClick = onBack) {
                Text("Back")
            }
        }

        return
    }

    val previewView = remember {
        androidx.camera.view.PreviewView(context)
    }

    AndroidView(
        factory = {
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )

    LaunchedEffect(Unit) {

        val cameraProviderFuture =
            androidx.camera.lifecycle.ProcessCameraProvider
                .getInstance(context)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                androidx.camera.core.Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider =
                            previewView.surfaceProvider
                    }

            val scanner =
                BarcodeScanning.getClient()

            val imageAnalysis =
                androidx.camera.core.ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context)
            ) { imageProxy ->

                val mediaImage =
                    imageProxy.image

                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val image =
                    InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->

                        val value =
                            barcodes.firstOrNull()?.rawValue

                        if (value != null) {
                            try {
                                val qrUri = Uri.parse(value)

                                if (qrUri.scheme == "upi") {

                                    val pa =
                                        qrUri.getQueryParameter("pa")

                                    val am =
                                        qrUri.getQueryParameter("am")

                                    if (!pa.isNullOrBlank()) {

                                        cameraProvider.unbindAll()

                                        onResult(pa, am)
                                    }
                                }

                            } catch (_: Exception) {
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

        }, ContextCompat.getMainExecutor(context))
    }


}