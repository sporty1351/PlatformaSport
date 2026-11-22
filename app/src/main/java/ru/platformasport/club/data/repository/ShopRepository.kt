package ru.platformasport.club.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ProductCategory(val title: String) {
    NUTRITION("Питание"), DRINKS("Напитки"), ACCESSORIES("Аксессуары")
}

@Serializable
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val category: ProductCategory,
    val priceRub: Int? = null,
    val available: Boolean = true
)

/** Локальная витрина. Для общего каталога клуба нужен серверный источник. */
class ShopRepository(context: Context) {
    private val preferences = context.getSharedPreferences("club_shop", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val defaults = listOf(
        Product("protein", "Протеиновый коктейль", "Вкус и состав уточняйте на ресепшене.", ProductCategory.NUTRITION),
        Product("bar", "Протеиновый батончик", "Перекус до или после тренировки. Состав — на упаковке.", ProductCategory.NUTRITION),
        Product("water", "Питьевая вода", "Возьмите с собой на тренировку.", ProductCategory.DRINKS),
        Product("towel", "Полотенце", "Наличие и условия получения уточняйте на ресепшене.", ProductCategory.ACCESSORIES),
        Product("slippers", "Тапочки", "Размеры и наличие уточняйте у сотрудника клуба.", ProductCategory.ACCESSORIES)
    )
    private val _products = MutableStateFlow(
        preferences.getString("products", null)?.let {
            runCatching { json.decodeFromString<List<Product>>(it) }.getOrNull()
        } ?: defaults
    )
    val products = _products.asStateFlow()

    fun save(product: Product) {
        require(product.title.isNotBlank())
        require(product.priceRub == null || product.priceRub >= 0)
        publish(if (_products.value.any { it.id == product.id })
            _products.value.map { if (it.id == product.id) product else it }
        else _products.value + product)
    }

    fun delete(id: String) = publish(_products.value.filterNot { it.id == id })

    private fun publish(products: List<Product>) {
        preferences.edit().putString("products", json.encodeToString(products)).apply()
        _products.value = products
    }
}
