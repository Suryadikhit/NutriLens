package com.example.nutrilens.database



import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


data class HistoryItem(
    val barcode: String,
    val productName: String?,
    val imageUrl: String? // ✅ Added image URL
)


class HistoryDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "history.db"
        private const val DATABASE_VERSION = 2 // ✅ Increment database version
        private const val TABLE_NAME = "search_history"
        private const val COLUMN_ID = "id"
        private const val COLUMN_BARCODE = "barcode"
        private const val COLUMN_PRODUCT_NAME = "product_name"
        private const val COLUMN_IMAGE_URL = "image_url" // ✅ Added new column for image URL
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BARCODE TEXT UNIQUE,
                $COLUMN_PRODUCT_NAME TEXT,
                $COLUMN_IMAGE_URL TEXT  -- ✅ New column for image URL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_IMAGE_URL TEXT") // ✅ Add column if upgrading
        }
    }

    fun insertHistory(barcode: String, productName: String?, imageUrl: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_BARCODE, barcode)
            put(COLUMN_PRODUCT_NAME, productName)
            put(COLUMN_IMAGE_URL, imageUrl) // ✅ Store image URL
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun getHistory(): List<HistoryItem> {
        val db = readableDatabase
        val historyList = mutableListOf<HistoryItem>()
        val cursor = db.rawQuery("SELECT $COLUMN_BARCODE, $COLUMN_PRODUCT_NAME, $COLUMN_IMAGE_URL FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)

        while (cursor.moveToNext()) {
            val barcode = cursor.getString(0)
            val productName = cursor.getString(1)
            val imageUrl = cursor.getString(2) // ✅ Fetch image URL
            historyList.add(HistoryItem(barcode, productName, imageUrl))
        }
        cursor.close()
        db.close()
        return historyList
    }

    fun deleteHistoryItem(barcode: String) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_BARCODE=?", arrayOf(barcode))
        db.close()
    }
}