package ru.platformasport.club.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.platformasport.club.data.remote.CrmAccount
import ru.platformasport.club.data.remote.FakeCrmApi
import ru.platformasport.club.ui.components.BrandLockup
import ru.platformasport.club.ui.components.ErrorBanner
import ru.platformasport.club.ui.components.PrimaryButton
import ru.platformasport.club.ui.theme.GraphiteLine
import ru.platformasport.club.ui.theme.GraphiteRaised
import ru.platformasport.club.ui.theme.PlatformaRed
import ru.platformasport.club.ui.theme.TextMuted
import ru.platformasport.club.ui.theme.TextPrimary
import ru.platformasport.club.ui.theme.TextSecondary
import ru.platformasport.club.ui.theme.TileWhite
import ru.platformasport.club.util.formatPhone
import ru.platformasport.club.util.plural

@Composable
fun AuthFlow(onAuthorized: (CrmAccount) -> Unit) {
    val viewModel: AuthViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()

    LaunchedEffect(account) {
        account?.let(onAuthorized)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible = state.step != AuthStep.PHONE) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { viewModel.back() }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Назад", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }
        }

        Spacer(Modifier.height(if (state.step == AuthStep.PHONE) 56.dp else 12.dp))
        BrandLockup(
            modifier = Modifier.fillMaxWidth(),
            markSize = if (state.step == AuthStep.PHONE) 84.dp else 56.dp
        )
        Spacer(Modifier.height(44.dp))

        when (state.step) {
            AuthStep.PHONE -> PhoneStep(state, viewModel)
            AuthStep.CODE -> CodeStep(state, viewModel)
            AuthStep.PROFILE -> ProfileStep(state, viewModel)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PhoneStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text(
        "Вход в клуб",
        style = MaterialTheme.typography.headlineMedium,
        color = TextPrimary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Введите номер, который указан в вашей клубной карте. Пришлём код в SMS.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(Modifier.height(24.dp))

    PhoneField(
        digitsAfterCode = state.phoneDigits.drop(1),
        display = formatPhone(state.phoneDigits),
        onChange = viewModel::onPhoneChanged
    )

    Spacer(Modifier.height(18.dp))
    AgreementRow(checked = state.agreed, onToggle = viewModel::toggleAgreement)

    if (state.error != null) {
        Spacer(Modifier.height(16.dp))
        ErrorBanner(state.error)
    }

    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = "Получить код",
        modifier = Modifier.fillMaxWidth(),
        enabled = state.phoneReady,
        loading = state.loading,
        onClick = viewModel::requestCode
    )

    Spacer(Modifier.height(20.dp))
    DemoHint(
        listOf(
            "${formatPhone(FakeCrmApi.DEMO_MEMBER_PHONE.drop(1))} — клиент с абонементом",
            "${formatPhone(FakeCrmApi.DEMO_ADMIN_PHONE.drop(1))} — сотрудник клуба",
            "любой другой номер — регистрация нового клиента"
        )
    )
}

@Composable
private fun CodeStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text("Код из SMS", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
    Spacer(Modifier.height(8.dp))
    Text(
        "Отправили на ${formatPhone(state.phoneDigits)}",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(Modifier.height(28.dp))

    CodeField(code = state.code, onChange = viewModel::onCodeChanged)

    if (state.error != null) {
        Spacer(Modifier.height(20.dp))
        ErrorBanner(state.error)
    }

    Spacer(Modifier.height(24.dp))
    if (state.resendIn > 0) {
        Text(
            "Отправить код повторно можно через ${state.resendIn} " +
                plural(state.resendIn, "секунду", "секунды", "секунд"),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    } else {
        Text(
            "Отправить код ещё раз",
            style = MaterialTheme.typography.labelMedium,
            color = PlatformaRed,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { viewModel.requestCode() }
                .padding(vertical = 8.dp)
        )
    }

    Spacer(Modifier.height(20.dp))
    DemoHint(listOf("Код для демонстрации — ${FakeCrmApi.DEMO_CODE}"))
}

@Composable
private fun ProfileStep(state: AuthUiState, viewModel: AuthViewModel) {
    Text("Знакомимся", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
    Spacer(Modifier.height(8.dp))
    Text(
        "Номера ещё нет в базе клуба. Заполните анкету — администратор выпустит клубную карту.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
    Spacer(Modifier.height(24.dp))

    LabeledField("Имя", state.firstName, viewModel::onFirstName)
    Spacer(Modifier.height(14.dp))
    LabeledField("Фамилия", state.lastName, viewModel::onLastName)
    Spacer(Modifier.height(14.dp))
    LabeledField(
        "Почта для чеков (не обязательно)",
        state.email,
        viewModel::onEmail,
        keyboardType = KeyboardType.Email
    )

    if (state.error != null) {
        Spacer(Modifier.height(16.dp))
        ErrorBanner(state.error)
    }

    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = "Создать профиль",
        modifier = Modifier.fillMaxWidth(),
        enabled = state.profileReady,
        loading = state.loading,
        onClick = viewModel::register
    )
}

// ------------------------------------------------------------------- поля

@Composable
private fun PhoneField(
    digitsAfterCode: String,
    display: String,
    onChange: (String) -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    BasicTextField(
        value = digitsAfterCode,
        onValueChange = onChange,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        decorationBox = { _ ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GraphiteRaised)
                    .border(1.dp, GraphiteLine, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = display.ifEmpty { "+7" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (digitsAfterCode.isEmpty()) TextMuted else TextPrimary,
                    letterSpacing = 1.sp
                )
            }
        }
    )
}

@Composable
private fun CodeField(code: String, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    BasicTextField(
        value = code,
        onValueChange = onChange,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus),
        decorationBox = { _ ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) { index ->
                    val char = code.getOrNull(index)
                    val active = index == code.length
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GraphiteRaised)
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) PlatformaRed else GraphiteLine,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char?.toString() ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(PlatformaRed),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(GraphiteRaised)
                        .border(1.dp, GraphiteLine, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) { inner() }
            }
        )
    }
}

@Composable
private fun AgreementRow(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) PlatformaRed else Color.Transparent)
                .border(
                    1.dp,
                    if (checked) PlatformaRed else GraphiteLine,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = TileWhite,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Согласен на обработку персональных данных",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun DemoHint(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GraphiteRaised)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Демо-режим",
            style = MaterialTheme.typography.labelSmall,
            color = PlatformaRed,
            fontWeight = FontWeight.SemiBold
        )
        lines.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
