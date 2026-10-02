package com.noinupi.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedButton

private val StatusBg = Color(0xFF050908)
private val StatusAmber = Color(0xFFD5A84A)
private val StatusBright = Color(0xFFE7C96B)
private val StatusMuted = Color(0xFF8C743C)

@Composable
fun PaymentStatusScreen(
    merchantName: String,
    upiId: String,
    amount: String,
    status: String,
    onDone: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StatusBg)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "PAYMENT STATUS",
            color = StatusAmber,
            fontFamily = PixelFont,
            fontSize = 14.sp,
            letterSpacing = 2.sp
        )

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StatusAmber)
                .padding(18.dp)
        ) {

            Text(
                text = status.uppercase(),
                color = StatusBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            if (merchantName.isNotBlank()) {
                Text(
                    text = merchantName,
                    color = StatusBright,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            Text(
                text = upiId,
                color = StatusMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Text(
                text = "₹$amount",
                color = StatusBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
        }

        Spacer(
            modifier = Modifier.height(30.dp)
        )

        OutlinedButton(
            onClick = onDone,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                StatusAmber
            )
        ) {
            Text(
                text = "[ DONE ]",
                color = StatusAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}