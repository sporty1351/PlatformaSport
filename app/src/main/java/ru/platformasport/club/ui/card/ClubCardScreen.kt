package ru.platformasport.club.ui.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.model.MembershipStatus
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.EmptyState
import ru.platformasport.club.ui.components.StatusChip
import ru.platformasport.club.ui.components.drawRhombusMark
import ru.platformasport.club.ui.theme.GraphiteDeep
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.Success
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.ui.theme.Warning
import ru.platformasport.club.util.daysWord
import ru.platformasport.club.util.dayMonthYear
import ru.platformasport.club.util.formatCardNumber
@Composable
fun ClubCardScreen(viewModel: AppViewModel, onBuy: () -> Unit) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val card = session.card

    if (card == null) {
        EmptyState(
            title = "Карта ещё не выпущена",
            description = "Как только администратор оформит клубную карту, она появится здесь.",
            actionText = "Посмотреть тарифы",
            onAction = onBuy,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Для посещения предъявите карту на ресепшене",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))

        CardFace(
            holder = card.holder,
            number = card.number,
            tariff = card.tariffTitle,
            club = card.clubName,
            statusTitle = card.statusTitle,
            statusColor = when (card.status) {
                MembershipStatus.ACTIVE -> if (card.daysLeft <= 7) Warning else Success
                MembershipStatus.FROZEN -> Warning
                else -> PlatformaRed
            }
        )

        Spacer(Modifier.height(18.dp))



        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GraphiteRaised)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardFact(
                "Абонемент действует до",
                card.validUntil.dayMonthYear(),
                if (card.status == MembershipStatus.NONE) "—" else
                    "осталось ${card.daysLeft} ${daysWord(card.daysLeft)}"
            )
            CardFact("Заморозка", "${card.freezeDaysLeft} ${daysWord(card.freezeDaysLeft)}", "доступно в этом абонементе")
            CardFact("Гостевые визиты", card.guestVisitsLeft.toString(), "можно привести друга")
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CardFace(
    holder: String,
    number: String,
    tariff: String,
    club: String,
    statusTitle: String,
    statusColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.62f)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(GraphiteRaised, GraphiteDeep),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRhombusMark(
                alpha = 0.07f,
                centerOffset = Offset.Zero
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "ПЛАТФОРМА",
                        style = MaterialTheme.typography.titleMedium,
                        color = TileWhite,
                        letterSpacing = 3.sp
                    )
                    Text(
                        club,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                StatusChip(text = statusTitle, color = statusColor)
            }
            Column {
                Text(
                    formatCardNumber(number),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TileWhite,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        holder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        tariff,
                        style = MaterialTheme.typography.bodySmall,
                        color = PlatformaRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFact(label: String, value: String, caption: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
}
