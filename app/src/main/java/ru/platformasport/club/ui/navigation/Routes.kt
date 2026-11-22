package ru.platformasport.club.ui.navigation

object Routes {
    const val HOME = "home"
    const val SCHEDULE = "schedule"
    const val CARD = "card"
    const val SHOP = "shop"
    const val TARIFFS = "tariffs"
    const val PROFILE = "profile"

    const val TOP_UP = "top_up"
    const val HISTORY = "history"
    const val MY_BOOKINGS = "my_bookings"
    const val ADMIN_SCHEDULE = "admin_schedule"

    const val CHECKOUT_PATTERN = "checkout/{tariffId}/{months}"
    fun checkout(tariffId: String, months: Int) = "checkout/$tariffId/$months"

    val bottomBar = setOf(HOME, SCHEDULE, SHOP, TARIFFS, PROFILE)
}
