package ru.platformasport.club.ui.tariffs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.TariffCatalog
import ru.platformasport.club.data.model.Tariff
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.PrimaryButton
import ru.platformasport.club.ui.components.RhombusBullet
import ru.platformasport.club.ui.components.ScreenTopBar
import ru.platformasport.club.ui.components.SelectableChip
import ru.platformasport.club.ui.components.StatusChip
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.util.asMoney
import ru.platformasport.club.util.monthsWord

@Composable
fun TariffsScreen(
    viewModel: AppViewModel,
    onCheckout: (String, Int) -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    var months by remember { mutableIntStateOf(12) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScreenTopBar(title = "Тарифы")

        Text(
            "Чем длиннее срок, тем дешевле месяц. Карта продлевается с даты окончания текущей.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TariffCatalog.periods) { period ->
                SelectableChip(
                    text = "$period ${monthsWord(period)}",
                    selected = months == period,
                    onClick = { months = period }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TariffCatalog.all, key = { it.id }) { tariff ->
                TariffCard(
                    tariff = tariff,
                    months = months,
                    current = session.card?.tariffTitle == tariff.title,
                    onClick = { onCheckout(tariff.id, months) }
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Цены действуют при оплате онлайн. Заморозка и гостевые визиты входят в тарифы с плюсом.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun TariffCard(
    tariff: Tariff,
    months: Int,
    current: Boolean,
    onClick: () -> Unit
) {
    val price = tariff.priceFor(months)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GraphiteRaised)
            .border(
                width = if (current) 1.dp else 0.dp,
                color = if (current) PlatformaRed else GraphiteRaised,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    tariff.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${tariff.timeWindow} · ${tariff.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            when {
                current -> StatusChip("Ваш тариф", PlatformaRed)
                tariff.badge != null -> StatusChip(tariff.badge, TileWhite)
                else -> Unit
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GraphiteLine)
        )
        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tariff.perks.take(3).forEach { perk ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RhombusBullet(size = 6.dp)
                    Text(perk, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    price.total.asMoney(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TileWhite
                )
                Text(
                    if (months == 1) "за месяц" else "${price.perMonth.asMoney()} в месяц",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                "$months ${monthsWord(months)}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = if (current) "Продлить" else "Оформить",
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        )
    }
}
