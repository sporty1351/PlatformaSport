package ru.platformasport.club.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.platformasport.club.data.local.AppPreferences
import ru.platformasport.club.data.model.ClassCategory
import ru.platformasport.club.data.model.ClassEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Расписание групповых занятий и записи пользователя.
 *
 * Администратор клуба редактирует сетку прямо в приложении или загружает её
 * одной пачкой (CSV). В боевой версии сюда же встанет синхронизация с
 * внешней системой — метод [replaceAll] для этого и предназначен.
 */
class ScheduleRepository(private val prefs: AppPreferences) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableStateFlow<List<ClassEvent>>(emptyList())
    val events: StateFlow<List<ClassEvent>> = _events.asStateFlow()

    private val _bookings = MutableStateFlow<Set<String>>(emptySet())
    val bookings: StateFlow<Set<String>> = _bookings.asStateFlow()

    init {
        scope.launch {
            _events.value = prefs.loadSchedule()?.takeIf { it.isNotEmpty() } ?: generateDefaultWeeks()
            _bookings.value = prefs.loadBookings()
        }
    }

    fun eventsOn(date: LocalDate): List<ClassEvent> =
        _events.value.filter { it.date == date }.sortedBy { it.start }

    fun book(eventId: String) {
        val event = _events.value.firstOrNull { it.id == eventId } ?: return
        if (event.isFull || eventId in _bookings.value) return
        persistEvents(_events.value.map { if (it.id == eventId) it.copy(booked = it.booked + 1) else it })
        persistBookings(_bookings.value + eventId)
    }

    fun cancelBooking(eventId: String) {
        if (eventId !in _bookings.value) return
        persistEvents(
            _events.value.map {
                if (it.id == eventId) it.copy(booked = (it.booked - 1).coerceAtLeast(0)) else it
            }
        )
        persistBookings(_bookings.value - eventId)
    }

    // ------------------------------------------------------- администрирование

    fun upsert(event: ClassEvent) {
        val exists = _events.value.any { it.id == event.id }
        persistEvents(
            if (exists) _events.value.map { if (it.id == event.id) event else it }
            else _events.value + event
        )
    }

    fun delete(eventId: String) {
        persistEvents(_events.value.filterNot { it.id == eventId })
        persistBookings(_bookings.value - eventId)
    }

    fun replaceAll(events: List<ClassEvent>) = persistEvents(events)

    fun resetToDefault() {
        persistEvents(generateDefaultWeeks())
        persistBookings(emptySet())
    }

    /**
     * Импорт сетки из CSV. Одна строка — одно занятие:
     * `дата;время;название;категория;тренер;зал;минуты;мест`
     * Например: `2026-09-21;18:30;Сайкл 45;CYCLE;Дарья Котова;Сайкл-зал;45;22`
     *
     * @param replaceRange удалить существующие занятия в дни, встретившиеся в файле.
     */
    fun importCsv(text: String, replaceRange: Boolean): Result<Int> {
        val parsed = mutableListOf<ClassEvent>()
        val problems = mutableListOf<String>()

        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEachIndexed { index, line ->
                val parts = line.split(';', ',').map { it.trim() }
                if (parts.size < 8) {
                    problems += "строка ${index + 1}: нужно 8 полей, найдено ${parts.size}"
                    return@forEachIndexed
                }
                runCatching {
                    ClassEvent(
                        id = UUID.randomUUID().toString(),
                        date = LocalDate.parse(parts[0]),
                        start = LocalTime.parse(parts[1]),
                        title = parts[2],
                        category = ClassCategory.valueOf(parts[3].uppercase()),
                        coach = parts[4],
                        room = parts[5],
                        durationMin = parts[6].toInt(),
                        capacity = parts[7].toInt()
                    )
                }.onSuccess { parsed += it }
                    .onFailure { problems += "строка ${index + 1}: ${it.message ?: "неверный формат"}" }
            }

        if (parsed.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    problems.take(3).joinToString("\n").ifEmpty { "Не нашли ни одного занятия" }
                )
            )
        }

        val affectedDates = parsed.map { it.date }.toSet()
        val kept = if (replaceRange) _events.value.filterNot { it.date in affectedDates } else _events.value
        persistEvents(kept + parsed)
        return Result.success(parsed.size)
    }

    fun csvTemplate(): String {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(1)
        return buildString {
            appendLine("# дата;время;название;категория;тренер;зал;минуты;мест")
            appendLine("# категории: " + ClassCategory.entries.joinToString(", ") { it.name })
            appendLine("$monday;09:00;Функциональный тренинг;STRENGTH;Артём Гущин;Зал групповых программ;55;20")
            appendLine("$monday;18:30;Сайкл 45;CYCLE;Дарья Котова;Сайкл-зал;45;22")
            append("${monday.plusDays(1)};19:00;Йога Флоу;MIND;Марина Юсупова;Зал йоги;60;16")
        }
    }

    // ------------------------------------------------------------- внутреннее

    private fun persistEvents(events: List<ClassEvent>) {
        val sorted = events.sortedWith(compareBy({ it.date }, { it.start }))
        _events.value = sorted
        scope.launch { prefs.saveSchedule(sorted) }
    }

    private fun persistBookings(ids: Set<String>) {
        _bookings.value = ids
        scope.launch { prefs.saveBookings(ids) }
    }

    /** Стартовая сетка на три недели вокруг сегодняшнего дня. */
    private fun generateDefaultWeeks(): List<ClassEvent> {
        val start = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1)
        val events = mutableListOf<ClassEvent>()
        repeat(21) { offset ->
            val date = start.plusDays(offset.toLong())
            TEMPLATE[date.dayOfWeek].orEmpty().forEach { slot ->
                events += ClassEvent(
                    id = "seed-$date-${slot.time}-${slot.title.hashCode()}",
                    title = slot.title,
                    category = slot.category,
                    coach = slot.coach,
                    room = slot.room,
                    date = date,
                    start = LocalTime.parse(slot.time),
                    durationMin = slot.duration,
                    capacity = slot.capacity,
                    booked = (slot.title.length * 3 + offset * 2) % (slot.capacity + 1),
                    level = slot.level,
                    description = slot.description
                )
            }
        }
        return events.sortedWith(compareBy({ it.date }, { it.start }))
    }

    private data class Slot(
        val time: String,
        val title: String,
        val category: ClassCategory,
        val coach: String,
        val room: String,
        val duration: Int,
        val capacity: Int,
        val level: String = "Любой уровень",
        val description: String = ""
    )

    private companion object {
        val TEMPLATE: Map<DayOfWeek, List<Slot>> = mapOf(
            DayOfWeek.MONDAY to listOf(
                Slot("07:30", "Утренний старт", ClassCategory.CARDIO, "Артём Гущин", "Зал групповых программ", 45, 20, description = "Разгон на день: кардиоблок и мобилизация суставов."),
                Slot("10:00", "Пилатес", ClassCategory.MIND, "Марина Юсупова", "Зал йоги", 55, 16),
                Slot("18:00", "Функциональный тренинг", ClassCategory.STRENGTH, "Сергей Пантелеев", "Зал групповых программ", 55, 22, "Средний", "Работа с собственным весом, гири, петли TRX."),
                Slot("19:15", "Сайкл 45", ClassCategory.CYCLE, "Дарья Котова", "Сайкл-зал", 45, 24, description = "Интервальная езда под музыку, контроль пульса."),
                Slot("20:30", "Стретчинг", ClassCategory.MIND, "Анна Реброва", "Зал йоги", 50, 18)
            ),
            DayOfWeek.TUESDAY to listOf(
                Slot("08:00", "Аквааэробика", ClassCategory.WATER, "Анна Реброва", "Бассейн", 45, 14),
                Slot("11:00", "Здоровая спина", ClassCategory.MIND, "Марина Юсупова", "Зал йоги", 50, 16),
                Slot("18:30", "Бокс", ClassCategory.FIGHT, "Игорь Мельник", "Бойцовский клуб", 60, 16, "Средний", "Техника ударов, работа на лапах и мешках."),
                Slot("19:00", "Zumba", ClassCategory.DANCE, "Дарья Котова", "Танцевальный класс", 55, 25),
                Slot("20:15", "Табата", ClassCategory.CARDIO, "Сергей Пантелеев", "Зал групповых программ", 30, 20, "Продвинутый")
            ),
            DayOfWeek.WEDNESDAY to listOf(
                Slot("07:30", "Сайкл Power", ClassCategory.CYCLE, "Дарья Котова", "Сайкл-зал", 45, 24, "Продвинутый"),
                Slot("10:30", "Детский фитнес", ClassCategory.KIDS, "Анна Реброва", "Детский центр", 45, 14, "5–8 лет"),
                Slot("18:00", "Hot Iron", ClassCategory.STRENGTH, "Артём Гущин", "Зал групповых программ", 55, 20, "Средний", "Штанга малого веса, большое количество повторов."),
                Slot("19:30", "Йога Флоу", ClassCategory.MIND, "Марина Юсупова", "Зал йоги", 60, 18),
                Slot("20:00", "Плавание: техника", ClassCategory.WATER, "Игорь Мельник", "Бассейн", 45, 12)
            ),
            DayOfWeek.THURSDAY to listOf(
                Slot("08:00", "Утренний старт", ClassCategory.CARDIO, "Сергей Пантелеев", "Зал групповых программ", 45, 20),
                Slot("12:00", "MFR: работа с фасцией", ClassCategory.MIND, "Анна Реброва", "Зал йоги", 50, 14),
                Slot("18:30", "Функциональный тренинг", ClassCategory.STRENGTH, "Артём Гущин", "Зал групповых программ", 55, 22),
                Slot("19:15", "Сайкл 45", ClassCategory.CYCLE, "Дарья Котова", "Сайкл-зал", 45, 24),
                Slot("20:30", "Бокс: новички", ClassCategory.FIGHT, "Игорь Мельник", "Бойцовский клуб", 60, 16, "Начальный")
            ),
            DayOfWeek.FRIDAY to listOf(
                Slot("07:30", "Пилатес", ClassCategory.MIND, "Марина Юсупова", "Зал йоги", 55, 16),
                Slot("11:00", "Аквааэробика", ClassCategory.WATER, "Анна Реброва", "Бассейн", 45, 14),
                Slot("18:00", "Кроссфит", ClassCategory.STRENGTH, "Сергей Пантелеев", "Зал групповых программ", 60, 18, "Продвинутый"),
                Slot("19:30", "Latina", ClassCategory.DANCE, "Дарья Котова", "Танцевальный класс", 55, 25),
                Slot("20:45", "Стретчинг", ClassCategory.MIND, "Марина Юсупова", "Зал йоги", 50, 18)
            ),
            DayOfWeek.SATURDAY to listOf(
                Slot("10:00", "Сайкл 60", ClassCategory.CYCLE, "Дарья Котова", "Сайкл-зал", 60, 24),
                Slot("11:30", "Детский фитнес", ClassCategory.KIDS, "Анна Реброва", "Детский центр", 45, 14, "9–12 лет"),
                Slot("13:00", "Функциональный тренинг", ClassCategory.STRENGTH, "Артём Гущин", "Зал групповых программ", 55, 22),
                Slot("16:00", "Йога Нидра", ClassCategory.MIND, "Марина Юсупова", "Зал йоги", 60, 18)
            ),
            DayOfWeek.SUNDAY to listOf(
                Slot("11:00", "Плавание: техника", ClassCategory.WATER, "Игорь Мельник", "Бассейн", 45, 12),
                Slot("12:30", "Табата", ClassCategory.CARDIO, "Сергей Пантелеев", "Зал групповых программ", 30, 20),
                Slot("17:00", "Йога Флоу", ClassCategory.MIND, "Анна Реброва", "Зал йоги", 60, 18)
            )
        )
    }
}
