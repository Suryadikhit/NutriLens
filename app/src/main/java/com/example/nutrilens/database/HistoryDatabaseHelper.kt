package com.example.nutrilens.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.nutrilens.viewmodel.Product
import org.json.JSONException
import org.json.JSONObject

data class HistoryItem(
    val barcode: String,
    val productName: String?,
    val imageUrl: String?,
    val brand: String?,
    val ingredients: String?,
    val nutrition: Map<String, Any>?,
    val nutriScore: String?,
    val novaScore: String?,
    val additives: String?,
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
        Log.d(TAG, "Database table created/updated: $TABLE_NAME")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Database version remains $DATABASE_VERSION, no upgrade performed.")
    }



// ✅ Insert or update product details
fun insertOrUpdateProduct(product: Product) {
    val db = writableDatabase
    try {
        val values = ContentValues().apply {
            put(COLUMN_BARCODE, product.barcode)
            put(COLUMN_PRODUCT_NAME, product.name)
            put(COLUMN_IMAGE_URL, product.imageUrl)
            put(COLUMN_BRAND, product.brand)
            put(COLUMN_INGREDIENTS, product.ingredients)
            put(COLUMN_NUTRITION, product.nutrition?.let { JSONObject(it as Map<*, *>).toString() })  // Convert Map to JSON string
            put(COLUMN_NUTRI_SCORE, product.nutriScore)
            put(COLUMN_NOVA_SCORE, product.novaScore)
            put(COLUMN_ADDITIVES, product.additives?.joinToString(", "))  // Convert List to CSV string
            put(COLUMN_PACKAGING, product.packaging)
            put(COLUMN_CARBON_FOOTPRINT, product.carbonFootprint)
            put(COLUMN_DESCRIPTION, product.quantity)  // Using quantity as description
        }
        val result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        Log.d(TAG, "Inserted/Updated product with barcode ${product.barcode}, result: $result")
    } catch (e: Exception) {
        Log.e(TAG, "Error inserting/updating product", e)
    } finally {
        db.close()
    }
}



    // ✅ Retrieve product details by barcode
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
                val quantity = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))  // Using DESCRIPTION as quantity

                product = Product(
                    barcode = barcode,
                    name = productName,
                    imageUrl = imageUrl,
                    brand = brand,
                    ingredients = ingredients,
                    nutrition = nutritionJson?.let { parseJsonToMap(it) },
                    nutriScore = nutriScore,
                    novaScore = novaScore,
                    additives = additives,
                    packaging = packaging,
                    carbonFootprint = carbonFootprint,
                    quantity = quantity  // Correctly assigning to quantity field
                )
                Log.d(TAG, "Product found: $product")
            } else {
                Log.d(TAG, "No product found with barcode: $barcode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving product by barcode", e)
        } finally {
            cursor.close()
            db.close()
        }

        return product
    }


    // ✅ Retrieve all history items
    fun getHistory(): List<HistoryItem> {
        val historyList = mutableListOf<HistoryItem>()
        val db = readableDatabase
        val query = """
        SELECT $COLUMN_BARCODE, $COLUMN_PRODUCT_NAME, $COLUMN_IMAGE_URL, 
               $COLUMN_BRAND, $COLUMN_INGREDIENTS, $COLUMN_NUTRITION, 
               $COLUMN_NUTRI_SCORE, $COLUMN_NOVA_SCORE, $COLUMN_ADDITIVES, 
               $COLUMN_PACKAGING, $COLUMN_CARBON_FOOTPRINT, $COLUMN_DESCRIPTION
        FROM $TABLE_NAME 
        ORDER BY $COLUMN_ID DESC
    """.trimIndent()

        db.rawQuery(query, null).use { cursor ->
            Log.d(TAG, "Fetching history items...")
            val barcodeIndex = cursor.getColumnIndexOrThrow(COLUMN_BARCODE)
            val productNameIndex = cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME)
            val imageUrlIndex = cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)
            val brandIndex = cursor.getColumnIndexOrThrow(COLUMN_BRAND)
            val ingredientsIndex = cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS)
            val nutritionIndex = cursor.getColumnIndexOrThrow(COLUMN_NUTRITION)
            val nutriScoreIndex = cursor.getColumnIndexOrThrow(COLUMN_NUTRI_SCORE)
            val novaScoreIndex = cursor.getColumnIndexOrThrow(COLUMN_NOVA_SCORE)
            val additivesIndex = cursor.getColumnIndexOrThrow(COLUMN_ADDITIVES)
            val packagingIndex = cursor.getColumnIndexOrThrow(COLUMN_PACKAGING)
            val carbonFootprintIndex = cursor.getColumnIndexOrThrow(COLUMN_CARBON_FOOTPRINT)
            val descriptionIndex = cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)

            while (cursor.moveToNext()) {
                val barcode = cursor.getString(barcodeIndex)
                val productName = cursor.getString(productNameIndex)
                val imageUrl = cursor.getString(imageUrlIndex)
                val brand = cursor.getString(brandIndex)
                val ingredients = cursor.getString(ingredientsIndex)
                val nutritionJson = cursor.getString(nutritionIndex)
                val nutriScore = cursor.getString(nutriScoreIndex)
                val novaScore = cursor.getString(novaScoreIndex)
                val additives = cursor.getString(additivesIndex)
                val packaging = cursor.getString(packagingIndex)
                val carbonFootprint = cursor.getString(carbonFootprintIndex)
                val description = cursor.getString(descriptionIndex)

                val nutrition = nutritionJson?.let { parseJsonToMap(it) }

                val historyItem = HistoryItem(
                    barcode, productName, imageUrl, brand, ingredients, nutrition,
                    nutriScore, novaScore, additives, packaging, carbonFootprint, description
                )
                historyList.add(historyItem)
                Log.d(TAG, "Fetched history item: $historyItem")
            }
        }

        Log.d(TAG, "Total history items fetched: ${historyList.size}")
        db.close()
        return historyList
    }


    // ✅ Delete history item
    fun deleteHistoryItem(barcode: String) {
        val db = writableDatabase
        try {
            val deletedRows = db.delete(TABLE_NAME, "$COLUMN_BARCODE=?", arrayOf(barcode))
            Log.d(TAG, "Deleted history item with barcode: $barcode, Rows affected: $deletedRows")
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
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.get(key)
            }
            map
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing JSON to Map", e)
            emptyMap()
        }
    }
}
