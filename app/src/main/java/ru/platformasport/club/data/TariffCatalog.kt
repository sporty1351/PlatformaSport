package ru.platformasport.club.data

import ru.platformasport.club.data.model.Tariff
import ru.platformasport.club.data.model.TariffPrice

/**
 * Прайс клуба. Сейчас зашит в приложение, чтобы концепт работал офлайн.
 * Когда появится внешняя система — этот объект заменяется на загрузку
 * из [ru.platformasport.club.data.remote.ExternalCrmApi], структура моделей не меняется.
 */
object TariffCatalog {

    val periods = listOf(1, 3, 6, 12)

    private fun prices(
        m1: Int,
        t3: Int, p3: Int,
        t6: Int, p6: Int,
        t12: Int, p12: Int
    ) = listOf(
        TariffPrice(1, m1, m1),
        TariffPrice(3, t3, p3),
        TariffPrice(6, t6, p6),
        TariffPrice(12, t12, p12)
    )

    private val PLUS_PERKS = listOf(
        "Бассейн и сауны",
        "Заморозка 30 дней",
        "2 гостевых визита"
    )
    private val BASE_PERKS = listOf(
        "Тренажёрный зал",
        "Групповые программы",
        "Кардиотеатр"
    )

    val all: List<Tariff> = listOf(
        Tariff(
            id = "base",
            title = "Базовая",
            timeWindow = "Любое время",
            subtitle = "Один клуб, сухие зоны",
            perks = BASE_PERKS,
            prices = prices(2900, 8400, 2800, 15600, 2600, 28800, 2400)
        ),
        Tariff(
            id = "base_plus",
            title = "Базовая+",
            timeWindow = "Любое время",
            subtitle = "Один клуб, все зоны",
            plus = true,
            badge = "Популярная",
            perks = BASE_PERKS + PLUS_PERKS,
            prices = prices(3100, 8850, 2950, 16200, 2700, 30000, 2500)
        ),
        Tariff(
            id = "base_plus_all",
            title = "Базовая+ все клубы",
            timeWindow = "Любое время",
            subtitle = "Сеть клубов, все зоны",
            plus = true,
            perks = BASE_PERKS + PLUS_PERKS + "Доступ во все клубы сети",
            prices = prices(3350, 9300, 3100, 17400, 2900, 31800, 2650)
        ),
        Tariff(
            id = "morning",
            title = "Энергичное утро",
            timeWindow = "06:00 — 11:00",
            subtitle = "Один клуб, сухие зоны",
            perks = BASE_PERKS,
            prices = prices(2200, 6300, 2100, 12300, 2050, 22800, 1900)
        ),
        Tariff(
            id = "morning_plus",
            title = "Энергичное утро+",
            timeWindow = "06:00 — 11:00",
            subtitle = "Один клуб, все зоны",
            plus = true,
            perks = BASE_PERKS + PLUS_PERKS,
            prices = prices(2350, 6600, 2200, 12900, 2150, 24000, 2000)
        ),
        Tariff(
            id = "day",
            title = "Выгодный день",
            timeWindow = "11:00 — 17:00",
            subtitle = "Один клуб, сухие зоны",
            perks = BASE_PERKS,
            prices = prices(2200, 6300, 2100, 12300, 2050, 22800, 1900)
        ),
        Tariff(
            id = "day_plus",
            title = "Выгодный день+",
            timeWindow = "11:00 — 17:00",
            subtitle = "Один клуб, все зоны",
            plus = true,
            perks = BASE_PERKS + PLUS_PERKS,
            prices = prices(2350, 6600, 2200, 12900, 2150, 24000, 2000)
        ),
        Tariff(
            id = "night",
            title = "Ночная",
            timeWindow = "21:00 — 00:00",
            subtitle = "Один клуб, сухие зоны",
            badge = "Минимальная цена",
            perks = BASE_PERKS,
            prices = prices(2100, 6000, 2000, 11700, 1950, 21000, 1750)
        ),
        Tariff(
            id = "night_plus",
            title = "Ночная+",
            timeWindow = "21:00 — 00:00",
            subtitle = "Один клуб, все зоны",
            plus = true,
            perks = BASE_PERKS + PLUS_PERKS,
            prices = prices(2300, 6300, 2100, 12300, 2050, 22200, 1850)
        ),
        Tariff(
            id = "offer",
            title = "Выгодное предложение",
            timeWindow = "06:00 — 17:00",
            subtitle = "Один клуб, сухие зоны",
            perks = BASE_PERKS,
            prices = prices(2400, 6900, 2300, 13500, 2250, 25200, 2100)
        ),
        Tariff(
            id = "offer_plus",
            title = "Выгодное предложение+",
            timeWindow = "06:00 — 17:00",
            subtitle = "Один клуб, все зоны",
            plus = true,
            perks = BASE_PERKS + PLUS_PERKS,
            prices = prices(2600, 7350, 2450, 14100, 2350, 26400, 2200)
        ),
        Tariff(
            id = "youth",
            title = "Молодёжная",
            timeWindow = "06:00 — 18:00",
            subtitle = "Для 14–17 лет, сухие зоны",
            badge = "14–17 лет",
            perks = BASE_PERKS + "Инструктаж для подростков",
            prices = prices(2250, 6450, 2150, 12300, 2050, 22800, 1900)
        )
    )

    fun byId(id: String): Tariff? = all.firstOrNull { it.id == id }
}
