package ru.platformasport.club.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.model.ClassEvent
import ru.platformasport.club.data.model.MembershipStatus
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.PrimaryButton
import ru.platformasport.club.ui.components.ProgressLine
import ru.platformasport.club.ui.components.RhombusBullet
import ru.platformasport.club.ui.components.SectionHeader
import ru.platformasport.club.ui.components.StatusChip
import ru.platformasport.club.ui.components.drawRhombusMark
import ru.platformasport.club.ui.theme.Graphite
import ru.platformasport.club.ui.theme.GraphiteDeep
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.Success
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.ui.theme.Warning
import ru.platformasport.club.util.asMoney
import ru.platformasport.club.util.dayMonth
import ru.platformasport.club.util.dayMonthYear
import ru.platformasport.club.util.daysWord
import ru.platformasport.club.util.hhmm
import ru.platformasport.club.util.placesWord
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onOpenCard: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenTariffs: () -> Unit,
    onTopUp: () -> Unit,
    onMyBookings: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()

    val profile = session.profile
    val card = session.card
    val upcoming = events.filter { !it.isPast }.take(8)
    val myNext = events
        .filter { it.id in bookings && !it.isPast }
        .minWithOrNull(compareBy({ it.date }, { it.start }))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))
            Text(
                greeting(),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                profile?.firstName.orEmpty().ifEmpty { "Гость" },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )

            Spacer(Modifier.height(18.dp))

            MembershipBanner(
                statusTitle = card?.statusTitle ?: "Нет абонемента",
                tariff = card?.tariffTitle ?: "Абонемент не оформлен",
                validUntil = card?.validUntil?.dayMonthYear(),
                daysLeft = card?.daysLeft ?: 0,
                totalDays = card?.totalDays ?: 0,
                active = card?.status == MembershipStatus.ACTIVE,
                frozen = card?.status == MembershipStatus.FROZEN,
                onClick = onOpenCard,
                onBuy = onOpenTariffs
            )

            Spacer(Modifier.height(12.dp))
            BalanceRow(balance = session.balance, onTopUp = onTopUp)

            Spacer(Modifier.height(20.dp))
            QuickActions(
                onSchedule = onOpenSchedule,
                onTariffs = onOpenTariffs,
                onTopUp = onTopUp,
                onBookings = onMyBookings
            )

            if (myNext != null) {
                Spacer(Modifier.height(20.dp))
                NextBooking(event = myNext, onClick = onMyBookings)
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title = "Ближайшие занятия",
                actionText = "Всё расписание",
                onAction = onOpenSchedule
            )
        }

        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(upcoming, key = { it.id }) { event ->
                UpcomingCard(event = event, booked = event.id in bookings, onClick = onOpenSchedule)
            }
        }

        Spacer(Modifier.height(26.dp))
        ClubInfo(Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(28.dp))
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

@Composable
private fun MembershipBanner(
    statusTitle: String,
    tariff: String,
    validUntil: String?,
    daysLeft: Int,
    totalDays: Int,
    active: Boolean,
    frozen: Boolean,
    onClick: () -> Unit,
    onBuy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(GraphiteRaised, GraphiteDeep),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .clickable(onClick = onClick)
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            drawRhombusMark(
                alpha = 0.06f,
                centerOffset = Offset(size.width * 0.34f, -size.height * 0.1f)
            )
        }
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Клубная карта",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tariff,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
                StatusChip(
                    text = statusTitle,
                    color = when {
                        frozen -> Warning
                        active && daysLeft > 7 -> Success
                        active -> Warning
                        else -> PlatformaRed
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (active || frozen) {
                Text(
                    "Действует до ${validUntil.orEmpty()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                ProgressLine(
                    fraction = if (totalDays > 0) daysLeft.toFloat() / totalDays else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    color = if (daysLeft <= 7) Warning else PlatformaRed
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Осталось $daysLeft ${daysWord(daysLeft)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysLeft <= 7) Warning else TextMuted
                )
            } else {
                Text(
                    "Выберите тариф — карта активируется сразу после оплаты.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    text = "Выбрать тариф",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onBuy
                )
            }
        }
    }
}

@Composable
private fun BalanceRow(balance: Int, onTopUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteRaised)
            .clickable(onClick = onTopUp)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Баланс", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                balance.asMoney(),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Пополнить",
                style = MaterialTheme.typography.labelMedium,
                color = PlatformaRed
            )
            Icon(
                Icons.Filled.AddCard,
                contentDescription = null,
                tint = PlatformaRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QuickActions(
    onSchedule: () -> Unit,
    onTariffs: () -> Unit,
    onTopUp: () -> Unit,
    onBookings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Записаться", Icons.Filled.CalendarMonth, Modifier.weight(1f), onSchedule)
            QuickAction("Купить тариф", Icons.Filled.Sell, Modifier.weight(1f), onTariffs)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("Пополнить баланс", Icons.Filled.AddCard, Modifier.weight(1f), onTopUp)
            QuickAction("Мои записи", Icons.Filled.Groups, Modifier.weight(1f), onBookings)
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GraphiteRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = PlatformaRed, modifier = Modifier.size(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun NextBooking(event: ClassEvent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlatformaRed.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RhombusBullet(size = 10.dp)
        Column(Modifier.weight(1f)) {
            Text(
                "Вы записаны: ${event.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${dayLabel(event.date)}, ${event.start.hhmm()} · ${event.room}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun UpcomingCard(event: ClassEvent, booked: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteRaised)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                event.start.hhmm(),
                style = MaterialTheme.typography.titleLarge,
                color = TileWhite,
                letterSpacing = 1.sp
            )
            Text(
                dayLabel(event.date),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Text(
            event.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            "${event.coach} · ${event.room}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(2.dp))
        if (booked) {
            StatusChip("Вы записаны", Success)
        } else {
            Text(
                if (event.isFull) "Мест нет" else "${event.freeSlots} ${placesWord(event.freeSlots)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (event.isFull) Warning else TextMuted
            )
        }
    }
}

@Composable
private fun ClubInfo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Graphite)
            .padding(2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GraphiteRaised)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Клуб", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            InfoLine("Ижевск, ул. 9 Января, 219А")
            InfoLine("Ежедневно 06:00 — 00:00")
            InfoLine("+7 (3412) 913-268")
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GraphiteLine)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Бассейн, сауны, тренажёрный зал, залы групповых программ, сайкл, бойцовский клуб, детский центр.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RhombusBullet(size = 6.dp, color = Color(0xFF6E6A68))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

internal fun dayLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "сегодня"
    LocalDate.now().plusDays(1) -> "завтра"
    else -> date.dayMonth()
}
