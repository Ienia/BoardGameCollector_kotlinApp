package com.example.gamecollector

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.getStringOrNull
import java.nio.file.Files.exists

//przechowuje informacje takie jak Id, tytuł, orginalny tytuł, tok publikacji, pozycje, zdjecie jednej gry
class Record{
    var id: Int = 0
    var title: String? = null
    var org_title: String? = null
    var year_pub: Int = 2001
    var full_pic: String? = null
    var pic: String? = null
    var expansion: Int = 0

    constructor( id: Int, title: String?, org_title: String?, year_pub: Int, full_pic: String?, pic: String?, exp: Int ) {
        this.id = id
        this.title = title
        this.org_title = org_title
        this.year_pub = year_pub
        this.full_pic = full_pic
        this.pic = pic
        this.expansion = exp
    }
}

enum class Orders{
    _ID, TITLE, YEAR_PUB
}

class MyDBHandler(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {
    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "gameDB.db"
        val TABLE_RECORDS = "games"
        val COLUMN_ID = "_id"
        val COLUMN_TITLE = "title"
        val COLUMN_ORIGINAL_TITLE = "original_title"
        val COLUMN_YEAR_PUB = "year_pub"
        val COLUMN_FULL_PIC = "full_pic"
        val COLUMN_THUMBNAIL = "pic"
        val COLUMN_EXPANSION = "expansion"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_GAMES_TABLE = ("CREATE TABLE " +
                TABLE_RECORDS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_TITLE + " TEXT," +
                COLUMN_ORIGINAL_TITLE + " TEXT," +
                COLUMN_YEAR_PUB + " INTEGER," +
                COLUMN_FULL_PIC + " TEXT," +
                COLUMN_THUMBNAIL + " TEXT, "+
                COLUMN_EXPANSION + " INTEGER"+
                ")")
        db.execSQL(CREATE_GAMES_TABLE)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS)
        onCreate(db)
    }

    //usuwanie wszystkiego
    fun clear(){
        val db = this.writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS)
        val CREATE_GAMES_TABLE = ("CREATE TABLE " +
                TABLE_RECORDS + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_TITLE + " TEXT," +
                COLUMN_ORIGINAL_TITLE + " TEXT," +
                COLUMN_YEAR_PUB + " INTEGER," +
                COLUMN_FULL_PIC + " TEXT," +
                COLUMN_THUMBNAIL + " TEXT, "+
                COLUMN_EXPANSION + " INTEGER"+
                ")")
        db.execSQL(CREATE_GAMES_TABLE)

    }
    //dodawanie rekordów
    fun addRecord(record: Record) {
        val values = ContentValues()
        values.put(COLUMN_ID, record.id)
        values.put(COLUMN_TITLE, record.title)
        values.put(COLUMN_ORIGINAL_TITLE, record.org_title)
        values.put(COLUMN_YEAR_PUB, record.year_pub)
        values.put(COLUMN_FULL_PIC, record.full_pic)
        values.put(COLUMN_THUMBNAIL, record.pic)
        values.put(COLUMN_EXPANSION, record.expansion)
        val db = this.writableDatabase
        db.insert(TABLE_RECORDS, null, values)
        db.close()
    }
    //znajdowanie po tytule
    fun findRecord(title: String?): Record? {
        val query =
            "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_TITLE LIKE \"$title\""
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        var rec: Record? = null

        if (cursor.moveToFirst()) {
            val id = Integer.parseInt(cursor.getString(0))
            val title = cursor.getString(1)
            val org_title = cursor.getString(2)
            val year_pub = cursor.getInt(3)
            val full_pic = cursor.getStringOrNull(4)
            val thumb = cursor.getStringOrNull(5)
            val exp = cursor.getInt(6)

            rec = Record(id, title, org_title, year_pub, full_pic, thumb, exp)
            cursor.close()
        }

        db.close()
        return rec
    }

    //znajdowanie po ID
    fun findRecord(id: Int): Record? {
        val query =
            "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_ID = $id"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        var rec: Record? = null

        if (cursor.moveToFirst()) {
            val id = Integer.parseInt(cursor.getString(0))
            val title = cursor.getString(1)
            val org_title = cursor.getString(2)
            val year_pub = cursor.getInt(3)
            val full_pic = cursor.getStringOrNull(4)
            val thumb = cursor.getStringOrNull(5)
            val exp = cursor.getInt(6)

            rec = Record(id, title, org_title, year_pub, full_pic, thumb, exp)
            cursor.close()
        }

        db.close()
        return rec
    }
    //ile gier w bazie?
    fun countGames():Int{
        var ans = 0
        val query =
            "SELECT COUNT(*) FROM $TABLE_RECORDS WHERE $COLUMN_EXPANSION = 0"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            ans = Integer.parseInt(cursor.getString(0))
            cursor.close()
        }
        db.close()
        return ans
    }
    //ile dodatkow w bazie?
    fun countAddons():Int{
        var ans = 0
        val query =
            "SELECT COUNT(*) FROM $TABLE_RECORDS WHERE $COLUMN_EXPANSION = 1"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            ans = Integer.parseInt(cursor.getString(0))
            cursor.close()
        }
        db.close()
        return ans
    }

    fun getVals(addOns: Int, ord: Orders = Orders._ID, desc: Boolean = false):List<Record>{
        val mList: MutableList<Record> = ArrayList()
        val d = if(desc) "DESC" else ""
        val o = ord.toString()
        val query =
            "SELECT * FROM $TABLE_RECORDS WHERE $COLUMN_EXPANSION = $addOns ORDER BY $o $d"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if(cursor.count == 0){
            return mList.toList()
        }
        cursor.moveToFirst()
        do {
            var rec: Record? = null
            val id = Integer.parseInt(cursor.getString(0))
            val title = cursor.getString(1)
            val org_title = cursor.getString(2)
            val year_pub = cursor.getInt(3)
            val full_pic = cursor.getStringOrNull(4)
            val thumb = cursor.getStringOrNull(5)
            rec = Record(id, title, org_title, year_pub, full_pic, thumb, addOns)
            mList.add(rec)
        }while(cursor.moveToNext())
        cursor.close()
        db.close()
        val res: List<Record> = mList
        return res
    }

    fun updateAddOn(id: String, exp: Int) {
        val rec = this.findRecord(Integer.parseInt(id))!!
        val values = ContentValues()
        values.put(COLUMN_ID, rec.id)
        values.put(COLUMN_TITLE, rec.title)
        values.put(COLUMN_ORIGINAL_TITLE, rec.org_title)
        values.put(COLUMN_YEAR_PUB, rec.year_pub)
        values.put(COLUMN_FULL_PIC, rec.full_pic)
        values.put(COLUMN_THUMBNAIL, rec.pic)
        values.put(COLUMN_EXPANSION, exp)
        val db = this.writableDatabase
        db.update(TABLE_RECORDS, values, "$COLUMN_ID=?", arrayOf(id))
        db.close()
    }

}

class Uris{
    var id_of_record: Int = 0
    var id_pic: Int = 0

    constructor(id_of_record: Int, id_pic: Int) {
        this.id_of_record = id_of_record
        this.id_pic = id_pic
    }
}

class DBPictures(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABSE_NAME, factory, DATABSE_VERSION) {
    companion object {
        private val DATABSE_VERSION = 1
        private val DATABSE_NAME = "pictures.db"
        val TABLE_PIC = "pic"
        val COLUMN_ID = "_id"
        val COLUMN_PIC_ID = "pic_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        crt(db)
    }
    fun crt(db: SQLiteDatabase){
        val CREATE_PIC_TABLE = ("CREATE TABLE " +
                TABLE_PIC + "(" +
                COLUMN_ID + " INTEGER," +
                COLUMN_PIC_ID + " INTEGER," +
                "PRIMARY KEY (" + COLUMN_ID +
                ") )")
        db.execSQL(CREATE_PIC_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_PIC)
        onCreate(db)
    }

    fun clear(){
        val db = this.writableDatabase
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_PIC)
        crt(db)
    }

    fun countPics(id: Int): Int{
        var answer = 0
        val query = "SELECT COUNT(*) FROM $TABLE_PIC WHERE $COLUMN_ID = $id"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if(cursor.moveToFirst()) {
            answer = Integer.parseInt(cursor.getString(0))
            cursor.close()
        }
        db.close()
        return answer
    }

    fun addPic(pic: Uris) {
        val values = ContentValues()
        values.put(COLUMN_ID, pic.id_of_record)
        values.put(COLUMN_PIC_ID, pic.id_pic)
        val db=this.writableDatabase
        db.insert(TABLE_PIC, null, values)
        db.close()
    }
    fun getPic(id: Int): List<Uris>{
        val mList: MutableList<Uris> = ArrayList()
        val query = "SELECT * FROM $TABLE_PIC WHERE $COLUMN_ID = $id"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if(cursor.count == 0) return mList.toList()
        cursor.moveToFirst()
        do {
            var u: Uris? = null
            val id = Integer.parseInt(cursor.getString(0))
            val pic_id = cursor.getInt(1)
            u = Uris(id, pic_id)
            mList.add(u)
        } while (cursor.moveToNext())
        cursor.close()
        db.close()
        val res: List<Uris> = mList
        return res
    }
}