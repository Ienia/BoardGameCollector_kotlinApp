package edu.put.inf151904

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.example.gamecollector.MyDBHandler
import com.example.gamecollector.Orders
import com.example.gamecollector.Record
import edu.put.inf151904.R
import java.util.concurrent.Executors


class GameActivity : AppCompatActivity() {

    var addons: Boolean = false
    var desc = false
    var actO = Orders._ID

    fun Boolean.toInt() = if (this) 1 else 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        val extras = intent.extras ?: return
        addons = extras.getBoolean("addins")
        val sortButton: Button = findViewById(R.id.sortButton)
        sortButton.text = "Sort by year of publication"
        Resort(Orders.TITLE)
    }

    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
    }

    fun showGames(l: List<Record>) {
        val table: TableLayout = findViewById(R.id.tableGames)
        for (i in 0..l.lastIndex) {
            val row = TableRow(this)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
            lp.setMargins(0, 10, 0, 10)
            row.layoutParams = lp

            val textViewId = TextView(this)
            textViewId.text = (i + 1).toString()
            textViewId.setPadding(20, 15, 20, 15)
            textViewId.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

            val im = ImageView(this)
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var image: Bitmap? = null

            executor.execute {
                val imageURL = l[i].full_pic
                try {
                    val `in` = java.net.URL(imageURL).openStream()
                    image = BitmapFactory.decodeStream(`in`)
                    handler.post {
                        im.setImageBitmap(image)
                    }
                } catch (e: Exception) {
                    val image = resources.getDrawable(R.drawable.ic_cards).toBitmap(150, 150)
                    im.setImageBitmap(image)
                    e.printStackTrace()
                }
            }


            val textViewName = TextView(this)
            textViewName.text = l[i].title.toString()
            textViewName.setPadding(20, 15, 20, 15)
            var textViewNameParams: TableRow.LayoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
            textViewNameParams.weight = 1.0f
            textViewName.layoutParams = textViewNameParams

            val textViewYear = TextView(this)
            textViewYear.text = l[i].year_pub.toString()
            textViewYear.setPadding(20, 15, 20, 15)
            textViewYear.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

            row.setOnClickListener() {
                callStats(l[i].id)
            }

            row.addView(textViewId)
            row.addView(im)
            row.addView(textViewName)
            row.addView(textViewYear)
            table.addView(row, lp)
        }
    }

    fun callStats(id: Int) {
        val i = Intent(this, GameStatsActivity::class.java)
        val b = Bundle()
        b.putInt("Id", id)
        i.putExtras(b)
        startActivity(i)
    }

    fun clear(){
        val table: TableLayout = findViewById(R.id.tableGames)
        while (table.childCount > 1) {
            table.removeView(table.getChildAt(table.childCount - 1))
        }
    }
    fun checkSet(new: Orders){
        if(actO == new){
            desc = desc xor true
        }
        else{
            desc = false
            actO = new
        }
    }
    fun Resort(what: Orders){
        clear()

        checkSet(what)

        val dbHandler = MyDBHandler(this, null, null,  1)
        val theList = dbHandler.getVals(addons.toInt(), actO, desc)
        showGames(theList)
    }
    fun sortClicked (v: View) {
        val sortButton: Button = findViewById(R.id.sortButton)
        if (sortButton.text == "Sort by year of publication") {
            sortButton.text = "Sort by title"
            Resort(Orders.YEAR_PUB)
        } else {
            sortButton.text = "Sort by year of publication"
            Resort(Orders.TITLE)
        }
    }
}