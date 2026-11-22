package ru.platformasport.club.ui.schedule

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.model.ClassCategory
import ru.platformasport.club.data.model.ClassEvent
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.EmptyState
import ru.platformasport.club.ui.components.ProgressLine
import ru.platformasport.club.ui.components.ScreenTopBar
import ru.platformasport.club.ui.components.SelectableChip
import ru.platformasport.club.ui.components.StatusChip
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
import ru.platformasport.club.util.hhmm
import ru.platformasport.club.util.placesWord
import ru.platformasport.club.util.weekdayShort
import java.time.LocalDate

@Composable
fun ScheduleScreen(viewModel: AppViewModel, onOpenAdmin: () -> Unit) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var category by remember { mutableStateOf<ClassCategory?>(null) }
    var membershipWarning by remember { mutableStateOf(false) }

    val days = remember { (0..20).map { LocalDate.now().plusDays(it.toLong()) } }
    val dayEvents = events
        .filter { it.date == selectedDate && (category == null || it.category == category) }
        .sortedBy { it.start }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScreenTopBar(
            title = "Расписание",
            actionIcon = if (session.profile?.isAdmin == true) Icons.Filled.EditCalendar else null,
            actionDescription = "Редактировать расписание",
            onAction = if (session.profile?.isAdmin == true) onOpenAdmin else null
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days, key = { it.toString() }) { date ->
                DayPill(
                    date = date,
                    selected = date == selectedDate,
                    hasEvents = events.any { it.date == date },
                    onClick = { selectedDate = date }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SelectableChip(
                    text = "Все",
                    selected = category == null,
                    onClick = { category = null }
                )
            }
            items(ClassCategory.entries.toList()) { item ->
                SelectableChip(
                    text = item.title,
                    selected = category == item,
                    onClick = { category = if (category == item) null else item }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (dayEvents.isEmpty()) {
            EmptyState(
                title = "На этот день занятий нет",
                description = "Выберите другой день или сбросьте фильтр по направлению."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        booked = event.id in bookings,
                        onToggle = {
                            when {
                                event.id in bookings -> viewModel.cancelBooking(event.id)
                                session.card?.isUsable != true -> membershipWarning = true
                                else -> viewModel.book(event.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (membershipWarning) {
        AlertDialog(
            onDismissRequest = { membershipWarning = false },
            containerColor = GraphiteRaised,
            title = { Text("Нужен действующий абонемент", color = TextPrimary) },
            text = {
                Text(
                    "Запись на групповые занятия открыта для клиентов с активной картой. Оформите тариф — запись откроется сразу после оплаты.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { membershipWarning = false }) {
                    Text("Понятно", color = PlatformaRed)
                }
            }
        )
    }
}

@Composable
private fun DayPill(
    date: LocalDate,
    selected: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) PlatformaRed else GraphiteRaised)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            date.weekdayShort(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TileWhite.copy(alpha = 0.85f) else TextMuted
        )
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = if (selected) TileWhite else TextPrimary
        )
        Box(
            Modifier
                .size(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    when {
                        !hasEvents -> GraphiteRaised
                        selected -> TileWhite
                        else -> PlatformaRed
                    }
                )
        )
    }
}

@Composable
private fun EventCard(event: ClassEvent, booked: Boolean, onToggle: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteRaised)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier.width(62.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    event.start.hhmm(),
                    style = MaterialTheme.typography.titleLarge,
                    color = TileWhite,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "${event.durationMin} мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .width(2.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (booked) PlatformaRed else GraphiteLine)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${event.coach} · ${event.room}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${event.category.title} · ${event.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            if (booked) {
                StatusChip("Записан", Success)
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                if (event.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Занято ${event.booked} из ${event.capacity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(Modifier.height(6.dp))
                ProgressLine(
                    fraction = if (event.capacity > 0) event.booked.toFloat() / event.capacity else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = GraphiteDeep,
                    color = if (event.isFull) Warning else PlatformaRed
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    event.isPast -> "Занятие прошло"
                    event.isFull && !booked -> "Свободных мест нет"
                    else -> "${event.freeSlots} ${placesWord(event.freeSlots)} свободно"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (event.isFull && !booked) Warning else TextMuted
            )
            BookingAction(
                booked = booked,
                enabled = !event.isPast && (booked || !event.isFull),
                onClick = onToggle
            )
        }
    }
}

@Composable
private fun BookingAction(booked: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val label = if (booked) "Отменить запись" else "Записаться"
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> GraphiteLine.copy(alpha = 0.4f)
                    booked -> GraphiteDeep
                    else -> PlatformaRed
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                !enabled -> TextMuted
                booked -> TextSecondary
                else -> TileWhite
            }
        )
    }
}
