package ru.platformasport.club.ui.tariffs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.TariffCatalog
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.OpState
import ru.platformasport.club.ui.components.EmptyState
import ru.platformasport.club.ui.components.ErrorBanner
import ru.platformasport.club.ui.components.InfoRow
import ru.platformasport.club.ui.components.OutlineButton
import ru.platformasport.club.ui.components.PrimaryButton
import ru.platformasport.club.ui.components.RhombusMark
import ru.platformasport.club.ui.components.ScreenTopBar
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.Success
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.util.asMoney
import ru.platformasport.club.util.dayMonthYear
import ru.platformasport.club.util.monthsWord
import java.time.LocalDate

private enum class PayMethod { BALANCE, BANK_CARD }

@Composable
fun CheckoutScreen(
    viewModel: AppViewModel,
    tariffId: String,
    months: Int,
    onBack: () -> Unit,
    onTopUp: () -> Unit,
    onDone: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val tariff = remember(tariffId) { TariffCatalog.byId(tariffId) }

    var method by remember { mutableStateOf(PayMethod.BALANCE) }
    var promo by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.resetOperation() }

    if (tariff == null) {
        EmptyState(
            title = "Тариф не найден",
            description = "Вернитесь к списку тарифов и выберите другой.",
            actionText = "Назад",
            onAction = onBack,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (operation is OpState.Success) {
        PurchaseResult(
            tariffTitle = tariff.title,
            validUntil = session.card?.validUntil,
            onDone = {
                viewModel.resetOperation()
                onDone()
            }
        )
        return
    }

    val price = tariff.priceFor(months)
    val card = session.card
    val startsFrom = card?.validUntil?.takeIf { it.isAfter(LocalDate.now()) } ?: LocalDate.now()
    val enoughBalance = session.balance >= price.total

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScreenTopBar(title = "Оформление", onBack = onBack)

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
                Text(tariff.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${tariff.timeWindow} · ${tariff.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(GraphiteLine)
                )
                InfoRow("Срок", "$months ${monthsWord(months)}")
                InfoRow("Цена месяца", price.perMonth.asMoney())
                InfoRow("Действует с", startsFrom.dayMonthYear())
                InfoRow(
                    "Действует до",
                    startsFrom.plusMonths(months.toLong()).dayMonthYear()
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("Оплата", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(10.dp))

            PayOption(
                icon = Icons.Filled.AccountBalanceWallet,
                title = "Баланс в приложении",
                subtitle = if (enoughBalance) {
                    "Доступно ${session.balance.asMoney()}"
                } else {
                    "Не хватает ${(price.total - session.balance).asMoney()}"
                },
                selected = method == PayMethod.BALANCE,
                enabled = true,
                onClick = { method = PayMethod.BALANCE }
            )
            Spacer(Modifier.height(10.dp))
            PayOption(
                icon = Icons.Filled.CreditCard,
                title = "Банковская карта",
                subtitle = "Оплата через платёжный сервис клуба",
                selected = method == PayMethod.BANK_CARD,
                enabled = true,
                onClick = { method = PayMethod.BANK_CARD }
            )

            if (method == PayMethod.BALANCE && !enoughBalance) {
                Spacer(Modifier.height(12.dp))
                OutlineButton(
                    text = "Пополнить баланс",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onTopUp
                )
            }

            Spacer(Modifier.height(18.dp))
            PromoField(value = promo, onChange = { promo = it })

            (operation as? OpState.Error)?.let {
                Spacer(Modifier.height(16.dp))
                ErrorBanner(it.message)
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(GraphiteRaised)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("К оплате", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    price.total.asMoney(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TileWhite
                )
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(
                text = "Оплатить",
                modifier = Modifier.fillMaxWidth(),
                enabled = card != null && (method == PayMethod.BANK_CARD || enoughBalance),
                loading = operation is OpState.Loading,
                onClick = {
                    viewModel.purchase(tariff, months, method == PayMethod.BALANCE)
                }
            )
        }
    }
}

@Composable
private fun PayOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GraphiteRaised)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) PlatformaRed else GraphiteRaised,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) PlatformaRed else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = PlatformaRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PromoField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GraphiteRaised)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (value.isBlank()) "Промокод" else value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isBlank()) TextMuted else TextPrimary
        )
        Text(
            "Ввести",
            style = MaterialTheme.typography.labelMedium,
            color = PlatformaRed,
            modifier = Modifier.clickable { onChange(if (value.isBlank()) "PLATFORMA10" else "") }
        )
    }
}

@Composable
private fun PurchaseResult(
    tariffTitle: String,
    validUntil: LocalDate?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RhombusMark(size = 76.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Абонемент оформлен",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            buildString {
                append(tariffTitle)
                if (validUntil != null) {
                    append(" действует до ")
                    append(validUntil.dayMonthYear())
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Карта уже обновилась — можно проходить через турникет.",
            style = MaterialTheme.typography.bodySmall,
            color = Success,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(32.dp))
        PrimaryButton(
            text = "Вернуться в клуб",
            modifier = Modifier.fillMaxWidth(),
            onClick = onDone
        )
    }
}
