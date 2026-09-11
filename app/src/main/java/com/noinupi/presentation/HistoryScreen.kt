package com.noinupi.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noinupi.domain.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HistoryBg = Color(0xFF050908)
private val HistoryAmber = Color(0xFFD5A84A)
private val HistoryBright = Color(0xFFE7C96B)
private val HistoryMuted = Color(0xFF8C743C)

@Composable
fun HistoryScreen(
    transactions: List<Transaction>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HistoryBg)
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = HistoryAmber
                )
            }

            Text(
                text = "TRANSACTION LOG",
                color = HistoryAmber,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                letterSpacing = 2.sp
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (transactions.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "NO TRANSACTIONS",
                    color = HistoryAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text = "TRANSACTION LOG IS EMPTY",
                    color = HistoryMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(transactions) { transaction ->
                    TransactionRow(transaction)
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction
) {

    val date = SimpleDateFormat(
        "dd/MM/yyyy  HH:mm",
        Locale.getDefault()
    ).format(
        Date(transaction.timestamp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                HistoryAmber
            )
            .padding(14.dp)
    ) {

        Text(
            text = "UPI ID",
            color = HistoryMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = transaction.upiId,
            color = HistoryBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "AMOUNT",
            color = HistoryMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = "₹${transaction.amount}",
            color = HistoryBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "DATE",
            color = HistoryMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = date,
            color = HistoryBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "STATUS : ${transaction.status.uppercase()}",
            color = HistoryAmber,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
    }
}