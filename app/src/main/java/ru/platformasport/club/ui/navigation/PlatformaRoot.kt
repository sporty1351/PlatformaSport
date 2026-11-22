package ru.platformasport.club.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.platformasport.club.ui.shop.ShopScreen
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.auth.AuthFlow
import ru.platformasport.club.ui.balance.TopUpScreen
import ru.platformasport.club.ui.balance.TransactionsScreen
import ru.platformasport.club.ui.card.ClubCardScreen
import ru.platformasport.club.ui.components.RhombusMark
import ru.platformasport.club.ui.home.HomeScreen
import ru.platformasport.club.ui.profile.ProfileScreen
import ru.platformasport.club.ui.schedule.AdminScheduleScreen
import ru.platformasport.club.ui.schedule.MyBookingsScreen
import ru.platformasport.club.ui.schedule.ScheduleScreen
import ru.platformasport.club.ui.tariffs.CheckoutScreen
import ru.platformasport.club.ui.tariffs.TariffsScreen
import ru.platformasport.club.ui.theme.Graphite
import ru.platformasport.club.ui.theme.GraphiteDeep
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TileWhite

@Composable
fun PlatformaRoot() {
    val appViewModel: AppViewModel = viewModel()
    val restored by appViewModel.sessionRestored.collectAsStateWithLifecycle()
    val session by appViewModel.session.collectAsStateWithLifecycle()

    when {
        !restored -> SplashScreen()
        !session.isAuthorized -> AuthFlow(onAuthorized = appViewModel::signIn)
        else -> MainShell(appViewModel)
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Graphite),
        contentAlignment = Alignment.Center
    ) {
        RhombusMark(size = 76.dp)
    }
}

@Composable
private fun MainShell(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        containerColor = Graphite,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute != null && currentRoute in Routes.bottomBar) {
                PlatformaBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = appViewModel,
                    onOpenCard = { navController.navigate(Routes.CARD) },
                    onOpenSchedule = { navController.navigateTab(Routes.SCHEDULE) },
                    onOpenTariffs = { navController.navigateTab(Routes.TARIFFS) },
                    onTopUp = { navController.navigate(Routes.TOP_UP) },
                    onMyBookings = { navController.navigate(Routes.MY_BOOKINGS) }
                )
            }
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    viewModel = appViewModel,
                    onOpenAdmin = { navController.navigate(Routes.ADMIN_SCHEDULE) }
                )
            }
            composable(Routes.SHOP) { ShopScreen(appViewModel) }
            composable(Routes.CARD) {
                ClubCardScreen(
                    viewModel = appViewModel,
                    onBuy = { navController.navigateTab(Routes.TARIFFS) }
                )
            }
            composable(Routes.TARIFFS) {
                TariffsScreen(
                    viewModel = appViewModel,
                    onCheckout = { tariffId, months ->
                        navController.navigate(Routes.checkout(tariffId, months))
                    }
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = appViewModel,
                    onBack = null,
                    onTopUp = { navController.navigate(Routes.TOP_UP) },
                    onHistory = { navController.navigate(Routes.HISTORY) },
                    onBookings = { navController.navigate(Routes.MY_BOOKINGS) },
                    onAdminSchedule = { navController.navigate(Routes.ADMIN_SCHEDULE) },
                    onBuyTariff = { navController.navigateTab(Routes.TARIFFS) }
                )
            }
            composable(Routes.TOP_UP) {
                TopUpScreen(viewModel = appViewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                TransactionsScreen(viewModel = appViewModel, onBack = { navController.popBackStack() })
            }
            composable(Routes.MY_BOOKINGS) {
                MyBookingsScreen(
                    viewModel = appViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenSchedule = { navController.navigateTab(Routes.SCHEDULE) }
                )
            }
            composable(Routes.ADMIN_SCHEDULE) {
                AdminScheduleScreen(viewModel = appViewModel, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.CHECKOUT_PATTERN,
                arguments = listOf(
                    navArgument("tariffId") { type = NavType.StringType },
                    navArgument("months") { type = NavType.IntType }
                )
            ) { entry ->
                CheckoutScreen(
                    viewModel = appViewModel,
                    tariffId = entry.arguments?.getString("tariffId").orEmpty(),
                    months = entry.arguments?.getInt("months") ?: 1,
                    onBack = { navController.popBackStack() },
                    onTopUp = { navController.navigate(Routes.TOP_UP) },
                    onDone = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun PlatformaBottomBar(navController: NavHostController, currentRoute: String?) {
    val left = listOf(
        TabItem(Routes.HOME, "Клуб", Icons.Filled.Home),
        TabItem(Routes.SCHEDULE, "Расписание", Icons.Filled.CalendarMonth)
    )
    val right = listOf(
        TabItem(Routes.TARIFFS, "Тарифы", Icons.Filled.Sell),
        TabItem(Routes.PROFILE, "Профиль", Icons.Filled.Person)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraphiteDeep)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GraphiteLine)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            left.forEach { Tab(it, currentRoute, Modifier.weight(1f), navController) }
            CardTab(
                selected = currentRoute == Routes.SHOP,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigateTab(Routes.SHOP) }
            )
            right.forEach { Tab(it, currentRoute, Modifier.weight(1f), navController) }
        }
    }
}

@Composable
private fun Tab(
    item: TabItem,
    currentRoute: String?,
    modifier: Modifier,
    navController: NavHostController
) {
    val selected = currentRoute == item.route
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { navController.navigateTab(item.route) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = if (selected) TileWhite else TextMuted,
            modifier = Modifier.size(23.dp)
        )
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TileWhite else TextMuted
        )
    }
}

/** Центральная вкладка каталога клуба. */
@Composable
private fun CardTab(selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.1f else 1f, label = "cardTabScale")
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            RhombusMark(size = 34.dp, alpha = if (selected) 1f else 0.5f)
        }
        Text(
            "Магазин",
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) TileWhite else TextMuted
        )
    }
}
