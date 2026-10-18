
package com.noinupi.presentation

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.compose.material3.TextButton

private val OnboardBg = Color(0xFF000000)
private val OnboardGold = Color(0xFFD5A84A)
private val OnboardBright = Color(0xFFE7C96B)
private val OnboardMuted = Color(0xFF8C743C)
private val OnboardPanel = Color(0xFF111111)
private val OnboardWhite = Color(0xFFE8E8E8)

private val OnboardPixelFont = PixelFont

@Composable
private fun PixelText(
    text: String,
    color: Color,
    size: Int,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = OnboardPixelFont,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = (size * 1.6f).sp,
        textAlign = align
    )
}

@Composable
fun BankingSetupScreen(onNext: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardBg)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            PixelText(
                text = "NOINUPI // SETUP 01",
                color = OnboardGold,
                size = 12
            )

            Spacer(Modifier.height(36.dp))

            PixelText(
                text = "ENABLE\nBANKING",
                color = OnboardBright,
                size = 23
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Set up banking\nwith *99#.",
                color = OnboardWhite,
                fontSize = 29.sp,
                lineHeight = 39.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OnboardPanel)
                    .padding(20.dp)
            ) {
                PixelText(
                    text = "ONE-TIME SETUP",
                    color = OnboardGold,
                    size = 12
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "1. Open the dialer using the button below.",
                    color = OnboardWhite,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "2. Follow your bank's instructions.",
                    color = OnboardWhite,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "3. Complete any required registration.",
                    color = OnboardWhite,
                    fontSize = 16.sp,
                    lineHeight = 25.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_DIAL,
                            "tel:*99%23".toUri()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OnboardGold),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OnboardBright
                )
            ) {
                PixelText(
                    text = "DIAL *99#",
                    color = OnboardBright,
                    size = 13
                )
            }
        }

        Column {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardGold,
                    contentColor = Color.Black
                )
            ) {
                PixelText(
                    text = "NEXT",
                    color = Color.Black,
                    size = 15
                )
            }
        }
    }
}

@Composable
fun AccessibilitySetupScreen(
    onGrant: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onRestrictedAccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardBg)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PixelText(
                    text = "NOINUPI // SETUP 02",
                    color = OnboardGold,
                    size = 10
                )

                TextButton(
                    onClick = {
                        onBack()
                    }
                ) {
                    Text(
                        text = "< BACK",
                        color = OnboardMuted,
                        fontFamily = OnboardPixelFont,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            PixelText(
                text = "PERMISSIONS",
                color = OnboardBright,
                size = 16
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Enable\nAccessibility.",
                color = OnboardWhite,
                fontSize = 31.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OnboardPanel)
                    .padding(20.dp)
            ) {
                PixelText(
                    text = "ACCESSIBILITY",
                    color = OnboardGold,
                    size = 14
                )

                Spacer(Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onGrant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OnboardGold),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OnboardBright
                    )
                ) {
                    PixelText(
                        text = "GRANT ACCESS",
                        color = OnboardBright,
                        size = 12
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(
                    "1. Click GRANT ACCESS",
                    "2. Click Downloaded Apps",
                    "3. Click NoInUPI",
                    "4. Enable NoInUPI USSD Automation",
                    "5. Return to NoInUPI and tap CONTINUE"
                ).forEach { step ->
                    Text(
                        text = step,
                        color = OnboardWhite,
                        fontSize = 14.sp,
                        lineHeight = 23.sp
                    )
                }
            }
            TextButton(
                onClick = onRestrictedAccess
            ) {
                Text(
                    text = "[?] RESTRICTED ACCESS?",
                    color = OnboardGold,
                    fontFamily = OnboardPixelFont,
                    fontSize = 9.sp
                )
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardGold,
                contentColor = Color.Black
            )
        ) {
            PixelText(
                text = "CONTINUE",
                color = Color.Black,
                size = 13
            )
        }
    }
}
@Composable
fun RestrictedAccessScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardBg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PixelText(
                    text = "NOINUPI // HELP",
                    color = OnboardGold,
                    size = 10
                )

                TextButton(onClick = onBack) {
                    Text(
                        text = "< BACK",
                        color = OnboardMuted,
                        fontFamily = OnboardPixelFont,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            PixelText(
                text = "RESTRICTED\nACCESS?",
                color = OnboardBright,
                size = 17
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Android may block accessibility access for apps installed outside the Play Store.",
                color = OnboardWhite,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(Modifier.height(16.dp))

            val steps = listOf(
                "01 / OPEN APP INFO" to "Open Settings > Apps > NoInUPI.",
                "02 / OPEN MENU" to "Tap the three-dot menu, if available.",
                "03 / ALLOW ACCESS" to "Tap Allow restricted settings and confirm.",
                "04 / ENABLE SERVICE" to
                        "Return to Accessibility Settings and enable NoInUPI USSD Automation."
            )

            steps.forEach { (heading, description) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OnboardPanel)
                        .padding(12.dp)
                ) {
                    PixelText(
                        text = heading,
                        color = OnboardGold,
                        size = 9
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = description,
                        color = OnboardWhite,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = "Settings may differ by phone and Android version.",
                color = OnboardMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, OnboardGold),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = OnboardBright
            )
        ) {
            PixelText(
                text = "BACK TO SETUP 02",
                color = OnboardBright,
                size = 9
            )
        }
    }
}
