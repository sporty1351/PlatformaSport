package ru.platformasport.club.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val RU: Locale = Locale.forLanguageTag("ru")

private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", RU)
private val DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("d MMMM yyyy", RU)
private val SHORT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", RU)
private val WEEKDAY_SHORT = DateTimeFormatter.ofPattern("EE", RU)
private val HH_MM = DateTimeFormatter.ofPattern("HH:mm", RU)

/** 28800 -> "28 800 ₽" */
fun Int.asMoney(): String = asNumber() + " \u20BD"

fun Int.asNumber(): String {
    val negative = this < 0
    val digits = kotlin.math.abs(this).toString()
    val grouped = digits.reversed().chunked(3).joinToString("\u00A0").reversed()
    return if (negative) "\u2212$grouped" else grouped
}

fun LocalDate.dayMonth(): String = format(DAY_MONTH)
fun LocalDate.dayMonthYear(): String = format(DAY_MONTH_YEAR)
fun LocalDate.short(): String = format(SHORT_DATE)
fun LocalDate.weekdayShort(): String = format(WEEKDAY_SHORT).replaceFirstChar { it.uppercase(RU) }
fun LocalTime.hhmm(): String = format(HH_MM)

/** Русская форма множественного числа: 1 день / 2 дня / 5 дней. */
fun plural(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

fun daysWord(count: Int): String = plural(count, "день", "дня", "дней")
fun monthsWord(count: Int): String = plural(count, "месяц", "месяца", "месяцев")
fun placesWord(count: Int): String = plural(count, "место", "места", "мест")

/** +7 (912) 345-67-89 из 11 цифр. */
fun formatPhone(digits: String): String {
    val d = digits.filter { it.isDigit() }.take(11)
    if (d.isEmpty()) return ""
    val sb = StringBuilder("+7")
    if (d.length > 1) sb.append(" (").append(d.substring(1, minOf(4, d.length)))
    if (d.length >= 5) sb.append(") ").append(d.substring(4, minOf(7, d.length)))
    if (d.length >= 8) sb.append("-").append(d.substring(7, minOf(9, d.length)))
    if (d.length >= 10) sb.append("-").append(d.substring(9, minOf(11, d.length)))
    return sb.toString()
}

/** 1234567890123 -> "1234 5678 9012 3" */
fun formatCardNumber(number: String): String = number.chunked(4).joinToString(" ")
