package ru.platformasport.club.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.EmptyState
import ru.platformasport.club.ui.components.ScreenTopBar
import ru.platformasport.club.ui.home.dayLabel
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.util.hhmm

@Composable
fun MyBookingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenSchedule: () -> Unit
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val bookings by viewModel.bookings.collectAsStateWithLifecycle()

    val mine = events
        .filter { it.id in bookings && !it.isPast }
        .sortedWith(compareBy({ it.date }, { it.start }))

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ScreenTopBar(title = "Мои записи", onBack = onBack)

        if (mine.isEmpty()) {
            EmptyState(
                title = "Записей пока нет",
                description = "Выберите занятие в расписании — место закрепится за вами.",
                actionText = "Открыть расписание",
                onAction = onOpenSchedule
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mine, key = { it.id }) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GraphiteRaised)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                event.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${dayLabel(event.date)}, ${event.start.hhmm()} · ${event.room}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                event.coach,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            "Отменить",
                            style = MaterialTheme.typography.labelMedium,
                            color = PlatformaRed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.cancelBooking(event.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
