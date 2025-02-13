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
    val nutrition: Map<String, Any>?
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

        private const val TAG = "HistoryDatabase"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BARCODE TEXT UNIQUE,
                $COLUMN_PRODUCT_NAME TEXT,
                $COLUMN_IMAGE_URL TEXT,
                $COLUMN_BRAND TEXT,
                $COLUMN_INGREDIENTS TEXT,
                $COLUMN_NUTRITION TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
        Log.d(TAG, "Database created: $TABLE_NAME")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
        Log.d(TAG, "Database upgraded to version $newVersion")
    }

    // ✅ Insert or update product details
    fun insertOrUpdateProduct(historyItem: Product) {
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_BARCODE, historyItem.barcode)
                put(COLUMN_PRODUCT_NAME, historyItem.name)
                put(COLUMN_IMAGE_URL, historyItem.imageUrl)
                put(COLUMN_BRAND, historyItem.brand)
                put(COLUMN_INGREDIENTS, historyItem.ingredients)
                put(COLUMN_NUTRITION, historyItem.nutrition?.let { JSONObject(it).toString() })
            }
            val result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "Inserted/Updated product: $historyItem, result: $result")
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

                product = Product(
                    barcode = barcode,
                    name = productName,
                    imageUrl = imageUrl,
                    brand = brand,
                    ingredients = ingredients,
                    nutrition = nutritionJson?.let { parseJsonToMap(it) }
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
                   $COLUMN_BRAND, $COLUMN_INGREDIENTS, $COLUMN_NUTRITION 
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

            while (cursor.moveToNext()) {
                val barcode = cursor.getString(barcodeIndex)
                val productName = cursor.getString(productNameIndex)
                val imageUrl = cursor.getString(imageUrlIndex)
                val brand = cursor.getString(brandIndex)
                val ingredients = cursor.getString(ingredientsIndex)
                val nutritionJson = cursor.getString(nutritionIndex)

                val nutrition = nutritionJson?.let { parseJsonToMap(it) }

                val historyItem = HistoryItem(barcode, productName, imageUrl, brand, ingredients, nutrition)
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
