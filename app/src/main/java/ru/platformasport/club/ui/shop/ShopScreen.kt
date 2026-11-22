package ru.platformasport.club.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.platformasport.club.data.repository.Product
import ru.platformasport.club.data.repository.ProductCategory
import ru.platformasport.club.ui.AppViewModel
import ru.platformasport.club.ui.components.*
import ru.platformasport.club.ui.theme.*
import ru.platformasport.club.util.asMoney
import java.util.UUID

@Composable
fun ShopScreen(viewModel: AppViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val admin = session.profile?.isAdmin == true
    var category by remember { mutableStateOf<ProductCategory?>(null) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    val shown = products.filter { category == null || it.category == category }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTopBar("Магазин", actionIcon = if (admin) Icons.Default.Add else null,
            actionDescription = "Добавить товар", onAction = if (admin) ({ creating = true }) else null)
        Text("Всё для тренировки", style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary, modifier = Modifier.padding(horizontal = 20.dp))
        Text("Питание, напитки и полезные мелочи. Покупка и получение — на ресепшене.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SelectableChip("Всё", category == null, onClick = { category = null }) }
            items(ProductCategory.entries.toList()) { entry ->
                SelectableChip(entry.title, category == entry, onClick = { category = entry })
            }
        }
        if (shown.isEmpty()) EmptyState("Пока нет товаров", "Попробуйте другую категорию.")
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.id }) { product ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(GraphiteRaised).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(GraphiteDeep), contentAlignment = Alignment.Center) {
                            Icon(when(product.category) {
                                ProductCategory.NUTRITION -> Icons.Default.Restaurant
                                ProductCategory.DRINKS -> Icons.Default.LocalDrink
                                ProductCategory.ACCESSORIES -> Icons.Default.ShoppingBag
                            }, null, tint = PlatformaRed, modifier = Modifier.size(28.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(product.category.title, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            Text(product.title, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(product.description, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Text(product.priceRub?.asMoney() ?: "Цена на ресепшене", color = TileWhite, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    StatusChip(if (product.available) "Уточните наличие на ресепшене" else "Временно недоступно",
                        if (product.available) TextSecondary else Warning)
                    if (admin) Row {
                        TextButton(onClick = { editing = product }) { Text("Изменить") }
                        TextButton(onClick = { deleting = product }) { Text("Удалить") }
                    }
                }
            }
        }
    }
    if (admin && (creating || editing != null)) ProductEditor(editing,
        onDismiss = { creating = false; editing = null },
        onSave = { viewModel.saveProduct(it); creating = false; editing = null })
    if (admin && deleting != null) AlertDialog(onDismissRequest = { deleting = null },
        title = { Text("Удалить товар?") }, text = { Text(deleting!!.title) },
        confirmButton = { TextButton(onClick = { viewModel.deleteProduct(deleting!!.id); deleting = null }) { Text("Удалить") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена") } })
}

@Composable
private fun ProductEditor(initial: Product?, onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var price by remember { mutableStateOf(initial?.priceRub?.toString().orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: ProductCategory.NUTRITION) }
    var available by remember { mutableStateOf(initial?.available ?: true) }
    val validPrice = price.isBlank() || price.toIntOrNull()?.let { it >= 0 } == true
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if(initial == null) "Новый товар" else "Изменить товар") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Описание") })
                OutlinedTextField(price, { price = it }, label = { Text("Цена, ₽ (необязательно)") },
                    singleLine = true, isError = !validPrice, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                ProductCategory.entries.forEach { entry ->
                    SelectableChip(entry.title, category == entry, onClick = { category = entry })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Доступен", Modifier.weight(1f))
                    Switch(available, { available = it })
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && validPrice, onClick = {
            onSave(Product(initial?.id ?: UUID.randomUUID().toString(), title.trim(), description.trim(), category, price.toIntOrNull(), available))
        }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}
