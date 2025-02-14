package com.example.nutrilens.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.nutrilens.viewmodel.Ingredient
import com.example.nutrilens.viewmodel.Product
import org.json.JSONObject

data class HistoryItem(
    val barcode: String,
    val productName: String?,
    val imageUrl: String?,
    val brand: String?,
    val ingredients: List<Ingredient>,
    val nutrition: Map<String, Any>?,
    val nutriScore: String?,
    val novaScore: String?,
    val additives: List<String>?,
    val packaging: String?,
    val carbonFootprint: String?,
    val description: String?
)

class HistoryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "history.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "search_history"
        private const val COLUMN_ID = "id"
        private const val COLUMN_BARCODE = "barcode"
        private const val COLUMN_PRODUCT_NAME = "product_name"
        private const val COLUMN_IMAGE_URL = "image_url"
        private const val COLUMN_BRAND = "brand"
        private const val COLUMN_INGREDIENTS = "ingredients"
        private const val COLUMN_NUTRITION = "nutrition"
        private const val COLUMN_NUTRI_SCORE = "nutri_score"
        private const val COLUMN_NOVA_SCORE = "nova_score"
        private const val COLUMN_ADDITIVES = "additives"
        private const val COLUMN_PACKAGING = "packaging"
        private const val COLUMN_CARBON_FOOTPRINT = "carbon_footprint"
        private const val COLUMN_DESCRIPTION = "description"
        private const val TAG = "HistoryDatabase"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BARCODE TEXT UNIQUE,
                $COLUMN_PRODUCT_NAME TEXT,
                $COLUMN_IMAGE_URL TEXT,
                $COLUMN_BRAND TEXT,
                $COLUMN_INGREDIENTS TEXT,
                $COLUMN_NUTRITION TEXT,
                $COLUMN_NUTRI_SCORE TEXT,
                $COLUMN_NOVA_SCORE TEXT,
                $COLUMN_ADDITIVES TEXT,
                $COLUMN_PACKAGING TEXT,
                $COLUMN_CARBON_FOOTPRINT TEXT,
                $COLUMN_DESCRIPTION TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        Log.d(TAG, "Database table created: $TABLE_NAME")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "No database upgrade needed.")
    }

    fun insertOrUpdateProduct(product: Product) {
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_BARCODE, product.barcode)
                put(COLUMN_PRODUCT_NAME, product.name)
                put(COLUMN_IMAGE_URL, product.imageUrl)
                put(COLUMN_BRAND, product.brand)
                put(COLUMN_INGREDIENTS, product.ingredients.joinToString(", ") { "${it.name} (${it.percentage}%)" })
                put(COLUMN_NUTRITION, product.nutrition?.let { JSONObject(it).toString() })
                put(COLUMN_NUTRI_SCORE, product.nutriScore)
                put(COLUMN_NOVA_SCORE, product.novaScore)
                put(COLUMN_ADDITIVES, product.additives?.joinToString(", "))
                put(COLUMN_PACKAGING, product.packaging)
                put(COLUMN_CARBON_FOOTPRINT, product.carbonFootprint)
                put(COLUMN_DESCRIPTION, product.quantity)
            }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "Product saved: ${product.barcode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving product", e)
        } finally {
            db.close()
        }
    }

    fun getProductByBarcode(barcode: String): Product? {
        val db = readableDatabase
        var product: Product? = null
        val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_BARCODE = ?"
        val cursor = db.rawQuery(query, arrayOf(barcode))

        try {
            if (cursor.moveToFirst()) {
                val productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL))
                val brand = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRAND))
                val ingredients = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS))
                val nutritionJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUTRITION))
                val nutriScore = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUTRI_SCORE))
                val novaScore = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOVA_SCORE))
                val additives = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDITIVES))?.split(", ")?.map { it.trim() }
                val packaging = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PACKAGING))
                val carbonFootprint = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARBON_FOOTPRINT))
                val quantity = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))

                product = Product(
                    barcode = barcode,
                    name = productName,
                    imageUrl = imageUrl,
                    brand = brand,
                    ingredients = ingredients.split(", ").map {
                        val name = it.substringBefore(" (")
                        val percentage = it.substringAfter(" (").removeSuffix("%)").toDoubleOrNull() ?: 0.0
                        Ingredient(name, percentage)
                    },
                    nutrition = nutritionJson?.let { parseJsonToMap(it) },
                    nutriScore = nutriScore,
                    novaScore = novaScore,
                    additives = additives,
                    packaging = packaging,
                    carbonFootprint = carbonFootprint,
                    quantity = quantity
                )
                Log.d(TAG, "Product retrieved: $product")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving product", e)
        } finally {
            cursor.close()
            db.close()
        }
        return product
    }

    fun getHistory(): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC"

        db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
                val barcode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE))
                val productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME))
                val imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL))
                val brand = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRAND))
                val ingredients = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS))
                val nutritionJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUTRITION))
                val nutriScore = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUTRI_SCORE))
                val novaScore = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOVA_SCORE))
                val additives = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDITIVES))?.split(", ")?.map { it.trim() }
                val packaging = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PACKAGING))
                val carbonFootprint = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARBON_FOOTPRINT))
                val description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))

                val nutrition = nutritionJson?.let { parseJsonToMap(it) }
                val ingredientList = ingredients.split(", ").map {
                    val name = it.substringBefore(" (")
                    val percentage = it.substringAfter(" (").removeSuffix("%)").toDoubleOrNull() ?: 0.0
                    Ingredient(name, percentage)
                }

                historyList.add(HistoryItem(barcode, productName, imageUrl, brand, ingredientList, nutrition, nutriScore, novaScore, additives, packaging, carbonFootprint, description))
            }
        }
        db.close()
        return historyList
    }

    fun deleteHistoryItem(barcode: String) {
        val db = writableDatabase
        try {
            db.delete(TABLE_NAME, "$COLUMN_BARCODE=?", arrayOf(barcode))
            Log.d(TAG, "History item deleted: $barcode")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting history item", e)
        } finally {
            db.close()
        }
    }

    private fun parseJsonToMap(jsonString: String): Map<String, Any> {
        return try {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, Any>()
            jsonObject.keys().forEach { key -> map[key] = jsonObject.get(key) }
            map
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error", e)
            emptyMap()
        }
    }
}
