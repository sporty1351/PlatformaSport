package ru.platformasport.club.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.model.MembershipStatus
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.InfoRow
import ru.platformasport.club.ui.components.PrimaryButton
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
import ru.platformasport.club.util.asMoney
import ru.platformasport.club.util.dayMonthYear
import ru.platformasport.club.util.daysWord
import ru.platformasport.club.util.formatCardNumber
import ru.platformasport.club.util.formatPhone

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onBack: (() -> Unit)?,
    onTopUp: () -> Unit,
    onHistory: () -> Unit,
    onBookings: () -> Unit,
    onAdminSchedule: () -> Unit,
    onBuyTariff: () -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val profile = session.profile
    val card = session.card

    var freezeDialog by remember { mutableStateOf(false) }
    var editDialog by remember { mutableStateOf(false) }
    var logoutDialog by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenTopBar(title = "Профиль", onBack = onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GraphiteRaised)
                    .clickable { editDialog = true }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(PlatformaRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        profile?.initials ?: "П",
                        style = MaterialTheme.typography.titleLarge,
                        color = TileWhite
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.fullName.orEmpty().ifEmpty { "Клиент клуба" },
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        formatPhone(profile?.phone.orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (profile?.email?.isNotBlank() == true) {
                        Text(
                            profile.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Редактировать",
                    tint = TextMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GraphiteRaised)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Абонемент", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    StatusChip(
                        text = card?.statusTitle ?: "Не оформлен",
                        color = when (card?.status) {
                            MembershipStatus.ACTIVE -> if ((card?.daysLeft ?: 0) <= 7) Warning else Success
                            MembershipStatus.FROZEN -> Warning
                            else -> PlatformaRed
                        }
                    )
                }
                InfoRow("Тариф", card?.tariffTitle ?: "—")
                InfoRow("Номер карты", card?.number?.let(::formatCardNumber) ?: "—")
                InfoRow(
                    "Действует до",
                    card?.takeIf { it.status != MembershipStatus.NONE }
                        ?.validUntil?.dayMonthYear() ?: "—"
                )
                InfoRow(
                    "Осталось",
                    card?.takeIf { it.status != MembershipStatus.NONE }
                        ?.let { "${it.daysLeft} ${daysWord(it.daysLeft)}" } ?: "—"
                )
                InfoRow("Заморозка", "${card?.freezeDaysLeft ?: 0} ${daysWord(card?.freezeDaysLeft ?: 0)}")

                Spacer(Modifier.height(12.dp))
                if (card?.status == MembershipStatus.NONE) {
                    PrimaryButton(
                        text = "Оформить абонемент",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBuyTariff
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GraphiteRaised)
                    .clickable(onClick = onTopUp)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Баланс", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(
                        session.balance.asMoney(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }
                Text("Пополнить", style = MaterialTheme.typography.labelMedium, color = PlatformaRed)
            }

            Spacer(Modifier.height(20.dp))

            MenuGroup {
                MenuItem(Icons.Filled.Groups, "Мои записи", onBookings)
                MenuDivider()
                MenuItem(Icons.Filled.ReceiptLong, "История операций", onHistory)
                MenuDivider()
                MenuItem(Icons.Filled.Sell, "Тарифы и продление", onBuyTariff)
                MenuDivider()
                MenuItem(
                    icon = Icons.Filled.AcUnit,
                    title = if (card?.status == MembershipStatus.FROZEN) {
                        "Возобновить абонемент"
                    } else {
                        "Заморозить абонемент"
                    },
                    onClick = {
                        if (card?.status == MembershipStatus.FROZEN) viewModel.unfreeze()
                        else freezeDialog = true
                    },
                    enabled = card != null && card.status != MembershipStatus.NONE
                )
            }

            Spacer(Modifier.height(14.dp))

            MenuGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.EditCalendar,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Режим администратора",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                            Text(
                                "Редактирование расписания клуба",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Switch(
                        checked = profile?.isAdmin == true,
                        onCheckedChange = viewModel::setAdminMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TileWhite,
                            checkedTrackColor = PlatformaRed,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = GraphiteDeep,
                            uncheckedBorderColor = GraphiteLine
                        )
                    )
                }
                if (profile?.isAdmin == true) {
                    MenuDivider()
                    MenuItem(Icons.Filled.EditCalendar, "Расписание клуба", onAdminSchedule)
                }
            }

            Spacer(Modifier.height(14.dp))

            MenuGroup {
                MenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Выйти из аккаунта",
                    onClick = { logoutDialog = true },
                    tint = PlatformaRed
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Спортивный клуб «Платформа» · Ижевск, ул. 9 Января, 219А\n+7 (3412) 913-268 · platformasport.ru",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Версия 1.0.0 · демо-режим",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(30.dp))
        }
    }

    if (freezeDialog) {
        FreezeDialog(
            available = card?.freezeDaysLeft ?: 0,
            onDismiss = { freezeDialog = false },
            onConfirm = {
                viewModel.freeze(it)
                freezeDialog = false
            }
        )
    }

    if (editDialog) {
        EditProfileDialog(
            firstName = profile?.firstName.orEmpty(),
            lastName = profile?.lastName.orEmpty(),
            email = profile?.email.orEmpty(),
            onDismiss = { editDialog = false },
            onSave = { first, last, mail ->
                viewModel.updateProfile(first, last, mail)
                editDialog = false
            }
        )
    }

    if (logoutDialog) {
        AlertDialog(
            onDismissRequest = { logoutDialog = false },
            containerColor = GraphiteRaised,
            title = { Text("Выйти из аккаунта?", color = TextPrimary) },
            text = {
                Text(
                    "Чтобы вернуться, понадобится снова подтвердить номер телефона.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    logoutDialog = false
                    viewModel.signOut()
                }) {
                    Text("Выйти", color = PlatformaRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { logoutDialog = false }) {
                    Text("Остаться", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GraphiteRaised)
    ) { content() }
}

@Composable
private fun MenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(1.dp)
            .background(GraphiteLine)
    )
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = TextSecondary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                if (tint == PlatformaRed) PlatformaRed else TextPrimary
            } else {
                TextMuted
            },
            modifier = Modifier.weight(1f)
        )
        if (tint != PlatformaRed) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FreezeDialog(
    available: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var days by remember { mutableStateOf(7) }
    val options = listOf(7, 14, 30).filter { it <= available }.ifEmpty { listOf(available) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteRaised,
        title = { Text("Заморозка абонемента", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Срок действия карты сдвинется на выбранное количество дней. Доступно $available ${daysWord(available)}.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { value ->
                        SelectableChip(
                            text = "$value ${daysWord(value)}",
                            selected = days == value,
                            modifier = Modifier.weight(1f),
                            onClick = { days = value }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = available > 0,
                onClick = { onConfirm(days) }
            ) {
                Text(
                    "Заморозить",
                    color = if (available > 0) PlatformaRed else TextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
private fun EditProfileDialog(
    firstName: String,
    lastName: String,
    email: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var first by remember { mutableStateOf(firstName) }
    var last by remember { mutableStateOf(lastName) }
    var mail by remember { mutableStateOf(email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteRaised,
        title = { Text("Ваши данные", color = TextPrimary) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.imePadding()
            ) {
                DialogField("Имя", first) { first = it }
                DialogField("Фамилия", last) { last = it }
                DialogField("Почта", mail) { mail = it }
                Text(
                    "Телефон и номер клубной карты меняются только на стойке клуба.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(first.trim(), last.trim(), mail.trim()) }) {
                Text("Сохранить", color = PlatformaRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
private fun DialogField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            cursorBrush = SolidColor(PlatformaRed),
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
