package edu.put.inf151904

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.gamecollector.DBPictures
import com.example.gamecollector.MyDBHandler
import com.example.gamecollector.Record
import com.example.gamecollector.Uris
import edu.put.inf151904.R
import java.io.File
import java.util.*
import java.util.concurrent.Executors


class GameStatsActivity : AppCompatActivity() {
    var Id: Int = 0
    var pic_id: Int = 0
    lateinit var imageView: ImageView
    var mGetContent = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { result ->
        if (result != null) {
            insertPhoto(result)
            val dbHandler = DBPictures(this, null, null,  1)
            pic_id += 1
            val record = Uris(Id, pic_id)
            dbHandler.addPic(record)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_stats)
        val extras = intent.extras
        Id = extras!!.getInt("Id")
        val dbm = MyDBHandler(this, null, null, 1)
        val rec = dbm.findRecord(Id)
        setCaptions(rec!!)

        val dbpic = DBPictures(this, null, null, 1)
        pic_id = dbpic.countPics(Id)
        showPhotos()

        imageView = findViewById(R.id.statPic)
        val button: Button = findViewById(R.id.photo)
        val buttonGallery: Button = findViewById(R.id.gallery)
        val tempImageUri = initTempUri()

        val resultLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) {
            insertPhoto(tempImageUri)
        }

        button.setOnClickListener {
            var tempImageUri = initTempUri()
            resultLauncher.launch(tempImageUri)
            val dbHandler = DBPictures(this, null, null,  1)
            pic_id += 1
            val record = Uris(Id, pic_id)
            dbHandler.addPic(record)
        }

        buttonGallery.setOnClickListener {
            mGetContent.launch("image/*")
        }

    }

    fun insertPhoto(result: Uri) {
        val table: TableLayout = findViewById(R.id.photos_table)
        val row = TableRow(this)
        val lp = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT)
        lp.setMargins(0, 10, 0, 10)
        row.layoutParams = lp

        val im1 = ImageView(this)
        im1.setImageBitmap(getCapturedImage(result))

        im1.setOnClickListener() {
            enlargePhoto(result)
        }

        val delete = Button(this)
        delete.text = "delete photo"
        delete.setPadding(20, 15, 20, 15)
        delete.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

        delete.setOnClickListener {
            deletePhoto(row)
        }

        row.addView(im1)
        row.addView(delete)
        table.addView(row, lp)
    }

    fun enlargePhoto (im: Uri) {
        val builder = Dialog(this)
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE)
        builder.setOnDismissListener(DialogInterface.OnDismissListener {
            //nothing;
        })
        val imageView = ImageView(this)
        imageView.setImageURI(im)
        builder.addContentView(
            imageView, RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        builder.show()
    }

    fun deletePhoto(row: TableRow) {
        row.removeAllViews()
        pic_id -= 1
    }

    fun stringCutter(src: String):String{
        var res: String = ""
        var lim = 10
        for(i in 0..src.length-1){
            if(src[i] != ' '){
                res += src[i]
            }
            else{
                if(i >= lim){
                    res+= '\n'
                    lim+=i
                }
                else{
                    res+=' '
                }
            }
        }
        return res
    }

    object BitmapScaler {
        fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
            val factor = width / b.height.toFloat()
            return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
        }
        fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
            val factor = height / b.height.toFloat()
            return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(), height, true)
        }
    }

    private fun getCapturedImage(selectedPhotoUri: Uri): Bitmap {
        val bitmap = when {
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                this.contentResolver,
                selectedPhotoUri
            )
            else -> {
                val source = ImageDecoder.createSource(this.contentResolver, selectedPhotoUri)
                ImageDecoder.decodeBitmap(source)
            }
        }
        return BitmapScaler.scaleToFitWidth(bitmap, 200)
    }

    private fun setCaptions(rec: Record) {
        val statYear: TextView = findViewById(R.id.statYear)
        statYear.text = "Pub: "+ rec.year_pub.toString()
        val statID: TextView = findViewById(R.id.statId)
        statID.text = rec.id.toString()
        val statName: TextView = findViewById(R.id.statName)
        statName.text = stringCutter(rec.title!!)
        val im: ImageView = findViewById(R.id.statPic)
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            val imageURL = rec.pic
            try {
                val `in` = java.net.URL(imageURL).openStream()
                val image: Bitmap? = BitmapFactory.decodeStream(`in`)
                val scaled_image = image?.let { BitmapScaler.scaleToFitWidth(it, 600) }
                handler.post {
                    im.setImageBitmap(scaled_image)
                }
            } catch (e: Exception) {
                image = BitmapFactory.decodeResource(
                    this.resources,
                    R.drawable.ic_cards
                )
                im.setImageBitmap(image)
                e.printStackTrace()
            }
        }
    }

    fun showPhotos() {
        val dbs = DBPictures(this, null, null, 1)
        try {
            for (i in 1..dbs.countPics(Id)) {
                val tempImage = File("$filesDir/temp_images/image$Id.$i.jpg")
                val uri = FileProvider.getUriForFile(applicationContext, getString(R.string.authorities), tempImage)
                insertPhoto(uri)
            }
        } catch (e: Exception) { }
    }

    private fun initTempUri(): Uri {
        val dbpic = DBPictures(this, null, null, 1)
        pic_id = dbpic.countPics(Id) + 1
        val tempImagesDir = File(applicationContext.filesDir, getString(R.string.temp_images_dir))
        tempImagesDir.mkdir()
        val tempImage = File("$filesDir/temp_images/image$Id.$pic_id.jpg")
        return FileProvider.getUriForFile(applicationContext, getString(R.string.authorities), tempImage)
    }

}