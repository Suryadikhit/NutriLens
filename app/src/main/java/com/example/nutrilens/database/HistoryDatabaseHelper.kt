package com.example.nutrilens.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No upgrade logic needed for version 1
    }

    fun insertHistory(
        barcode: String,
        productName: String?,
        imageUrl: String?,
        brand: String?,
        ingredients: String?,
        nutrition: Map<String, Any>?
    ) {
        val db = writableDatabase
        try {
            val values = ContentValues().apply {
                put(COLUMN_BARCODE, barcode)
                put(COLUMN_PRODUCT_NAME, productName)
                put(COLUMN_IMAGE_URL, imageUrl)
                put(COLUMN_BRAND, brand)
                put(COLUMN_INGREDIENTS, ingredients)
                put(COLUMN_NUTRITION, nutrition?.let { JSONObject(it).toString() })
            }
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

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

                historyList.add(HistoryItem(barcode, productName, imageUrl, brand, ingredients, nutrition))
            }
        }

        db.close()
        return historyList
    }

    fun deleteHistoryItem(barcode: String) {
        val db = writableDatabase
        try {
            db.delete(TABLE_NAME, "$COLUMN_BARCODE=?", arrayOf(barcode))
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
            emptyMap()
        }
    }
}
