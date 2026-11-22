package ru.platformasport.club.ui.schedule

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.model.ClassCategory
import ru.platformasport.club.data.model.ClassEvent
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.ErrorBanner
import ru.platformasport.club.ui.components.OutlineButton
import ru.platformasport.club.ui.components.PrimaryButton
import ru.platformasport.club.ui.components.ScreenTopBar
import ru.platformasport.club.ui.components.SelectableChip
import ru.platformasport.club.ui.theme.GraphiteDeep
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.Success
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.util.dayMonth
import ru.platformasport.club.util.hhmm
import ru.platformasport.club.util.weekdayShort
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private enum class AdminTab(val title: String) { LIST("Занятия"), IMPORT("Загрузка сетки") }

@Composable
fun AdminScheduleScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(AdminTab.LIST) }
    var editing by remember { mutableStateOf<ClassEvent?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        ScreenTopBar(
            title = "Расписание клуба",
            onBack = onBack,
            actionIcon = Icons.Filled.Add,
            actionDescription = "Добавить занятие",
            onAction = { creating = true }
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminTab.entries.forEach {
                SelectableChip(text = it.title, selected = tab == it, onClick = { tab = it })
            }
        }

        Spacer(Modifier.height(14.dp))

        when (tab) {
            AdminTab.LIST -> EventAdminList(
                events = events.filter { !it.date.isBefore(LocalDate.now()) },
                onEdit = { editing = it },
                onDelete = viewModel::deleteEvent
            )

            AdminTab.IMPORT -> ImportPanel(viewModel)
        }
    }

    if (creating || editing != null) {
        EventEditorDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = {
                viewModel.upsertEvent(it)
                creating = false
                editing = null
            }
        )
    }
}

@Composable
private fun EventAdminList(
    events: List<ClassEvent>,
    onEdit: (ClassEvent) -> Unit,
    onDelete: (String) -> Unit
) {
    if (events.isEmpty()) {
        Text(
            "Сетка пустая. Добавьте занятие вручную или загрузите её файлом.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(20.dp)
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val grouped = events.groupBy { it.date }.toSortedMap()
        grouped.forEach { (date, dayEvents) ->
            item(key = "header-$date") {
                Text(
                    "${date.weekdayShort()}, ${date.dayMonth()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PlatformaRed,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
            items(dayEvents.sortedBy { it.start }, key = { it.id }) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GraphiteRaised)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        event.start.hhmm(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TileWhite,
                        modifier = Modifier.width(52.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            event.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            "${event.coach} · ${event.room} · ${event.booked}/${event.capacity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    IconAction(Icons.Filled.Edit, "Изменить") { onEdit(event) }
                    IconAction(Icons.Filled.Delete, "Удалить") { onDelete(event.id) }
                }
            }
        }
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextSecondary, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun ImportPanel(viewModel: AppViewModel) {
    var csv by remember { mutableStateOf("") }
    var replaceRange by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(
            "Загрузите сетку на неделю одним текстом. Одна строка — одно занятие:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "дата;время;название;категория;тренер;зал;минуты;мест",
            style = MaterialTheme.typography.bodySmall,
            color = PlatformaRed
        )

        Spacer(Modifier.height(14.dp))
        BasicTextField(
            value = csv,
            onValueChange = { csv = it; error = null; message = null },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
            cursorBrush = SolidColor(PlatformaRed),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GraphiteDeep)
                        .border(1.dp, GraphiteLine, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    if (csv.isEmpty()) {
                        Text(
                            "2026-09-21;18:30;Сайкл 45;CYCLE;Дарья Котова;Сайкл-зал;45;22",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    inner()
                }
            }
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { replaceRange = !replaceRange }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (replaceRange) PlatformaRed else GraphiteRaised)
                    .border(1.dp, GraphiteLine, RoundedCornerShape(6.dp))
            )
            Text(
                "Заменить занятия в этих днях",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(error!!)
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Success.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(message!!, style = MaterialTheme.typography.bodyMedium, color = Success)
            }
        }

        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = "Опубликовать расписание",
            modifier = Modifier.fillMaxWidth(),
            enabled = csv.isNotBlank(),
            onClick = {
                viewModel.importSchedule(csv, replaceRange).fold(
                    onSuccess = {
                        message = "Загружено занятий: $it"
                        error = null
                        csv = ""
                    },
                    onFailure = {
                        error = it.message ?: "Не удалось разобрать файл"
                        message = null
                    }
                )
            }
        )
        Spacer(Modifier.height(10.dp))
        OutlineButton(
            text = "Вставить пример",
            modifier = Modifier.fillMaxWidth(),
            onClick = { csv = viewModel.csvTemplate() }
        )
        Spacer(Modifier.height(10.dp))
        OutlineButton(
            text = "Вернуть базовую сетку",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                viewModel.resetSchedule()
                message = "Расписание сброшено к типовой неделе"
                error = null
            }
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EventEditorDialog(
    initial: ClassEvent?,
    onDismiss: () -> Unit,
    onSave: (ClassEvent) -> Unit
) {
    var date by remember { mutableStateOf((initial?.date ?: LocalDate.now()).toString()) }
    var time by remember { mutableStateOf((initial?.start ?: LocalTime.of(18, 0)).toString().take(5)) }
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var coach by remember { mutableStateOf(initial?.coach.orEmpty()) }
    var room by remember { mutableStateOf(initial?.room.orEmpty()) }
    var duration by remember { mutableStateOf((initial?.durationMin ?: 55).toString()) }
    var capacity by remember { mutableStateOf((initial?.capacity ?: 20).toString()) }
    var category by remember { mutableStateOf(initial?.category ?: ClassCategory.STRENGTH) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteRaised,
        title = {
            Text(
                if (initial == null) "Новое занятие" else "Изменить занятие",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminField("Дата (ГГГГ-ММ-ДД)", date) { date = it }
                AdminField("Время (ЧЧ:ММ)", time) { time = it }
                AdminField("Название", title) { title = it }
                AdminField("Тренер", coach) { coach = it }
                AdminField("Зал", room) { room = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        AdminField("Минут", duration, KeyboardType.Number) { duration = it }
                    }
                    Box(Modifier.weight(1f)) {
                        AdminField("Мест", capacity, KeyboardType.Number) { capacity = it }
                    }
                }
                Text("Направление", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClassCategory.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { item ->
                                SelectableChip(
                                    text = item.title,
                                    selected = category == item,
                                    modifier = Modifier.weight(1f),
                                    onClick = { category = item }
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                AdminField("Описание", description) { description = it }
                if (formError != null) ErrorBanner(formError!!)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = runCatching {
                    ClassEvent(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        title = title.trim().ifEmpty { throw IllegalArgumentException("Укажите название") },
                        category = category,
                        coach = coach.trim().ifEmpty { "Тренер клуба" },
                        room = room.trim().ifEmpty { "Зал групповых программ" },
                        date = LocalDate.parse(date.trim()),
                        start = LocalTime.parse(time.trim()),
                        durationMin = duration.trim().toInt(),
                        capacity = capacity.trim().toInt(),
                        booked = initial?.booked ?: 0,
                        level = initial?.level ?: "Любой уровень",
                        description = description.trim()
                    )
                }
                parsed.fold(
                    onSuccess = onSave,
                    onFailure = { formError = it.message ?: "Проверьте поля" }
                )
            }) {
                Text("Сохранить", color = PlatformaRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AdminField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(PlatformaRed),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraphiteDeep)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) { inner() }
            }
        )
    }
}
