package ru.platformasport.club.ui.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.model.TxType
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.EmptyState
import ru.platformasport.club.ui.components.PrimaryButton
import ru.platformasport.club.ui.components.ScreenTopBar
import ru.platformasport.club.ui.components.SelectableChip
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.Success
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.util.asMoney
import ru.platformasport.club.util.dayMonth
import ru.platformasport.club.util.hhmm

private val PRESETS = listOf(500, 1000, 2000, 3000, 5000, 10000)

@Composable
fun TopUpScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    var preset by remember { mutableStateOf<Int?>(1000) }
    var custom by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }

    val amount = custom.filter { it.isDigit() }.toIntOrNull() ?: preset ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        ScreenTopBar(title = "Пополнение баланса", onBack = onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GraphiteRaised)
                    .padding(18.dp)
            ) {
                Text("Сейчас на балансе", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Text(
                    session.balance.asMoney(),
                    style = MaterialTheme.typography.displaySmall,
                    color = TileWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "С баланса оплачиваются абонементы, персональные тренировки и услуги клуба.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Сумма", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PRESETS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { value ->
                            SelectableChip(
                                text = value.asMoney(),
                                selected = custom.isBlank() && preset == value,
                                modifier = Modifier.weight(1f),
                                onClick = { preset = value; custom = ""; done = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = custom,
                onValueChange = { custom = it.filter { ch -> ch.isDigit() }.take(6); done = false },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = TextPrimary),
                cursorBrush = SolidColor(PlatformaRed),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GraphiteRaised)
                            .border(1.dp, GraphiteLine, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (custom.isEmpty()) {
                            Text(
                                "Другая сумма",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                        }
                        inner()
                    }
                }
            )

            if (done) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Success.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        "Баланс пополнен. Средства уже доступны.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Success
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(GraphiteRaised)
                .padding(20.dp)
        ) {
            PrimaryButton(
                text = if (amount > 0) "Пополнить на ${amount.asMoney()}" else "Выберите сумму",
                modifier = Modifier.fillMaxWidth(),
                enabled = amount > 0,
                onClick = {
                    viewModel.topUp(amount)
                    custom = ""
                    done = true
                }
            )
        }
    }
}

@Composable
fun TransactionsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val session by viewModel.session.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScreenTopBar(title = "История операций", onBack = onBack)

        if (session.transactions.isEmpty()) {
            EmptyState(
                title = "Операций пока нет",
                description = "Здесь появятся пополнения, покупки абонементов и оплата услуг клуба."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(session.transactions, key = { it.id }) { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GraphiteRaised)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tx.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                            Text(
                                "${tx.date.dayMonth()}, ${tx.time.hhmm()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            (if (tx.amount > 0) "+" else "") + tx.amount.asMoney(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (tx.type) {
                                TxType.TOPUP, TxType.REFUND -> Success
                                TxType.PURCHASE -> TextPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}
