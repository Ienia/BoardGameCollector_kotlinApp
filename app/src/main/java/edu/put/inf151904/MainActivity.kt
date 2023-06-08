package edu.put.inf151904

import LoadingDialog
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gamecollector.DBPictures
import com.example.gamecollector.MyDBHandler
import com.example.gamecollector.Record
import edu.put.inf151904.R
import kotlinx.coroutines.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.*
import java.lang.Runnable
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    private inner class DataDownloader: AsyncTask<String, Int, String>(){

        var u: String = ""
        var name: String = ""
        var expansion: Int = 0

        fun setData(adress: String, name: String, expansion: Int){
            this.u = adress
            this.name = name
            this.expansion = expansion
        }

        override fun onPreExecute() {
            super.onPreExecute()
            isDownloadingFinished = false
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            isDownloadingFinished = true
            afterSync()
            if(isSavingAddinsFinished)
                loading.isDismiss()
        }

        override fun doInBackground(vararg p0: String?): String {

            try {
                val url = URL(u)
                val connection = url.openConnection()
                connection.connect()
                val istream = connection.getInputStream()
                val content = istream.bufferedReader().use { it.readText() }
                val lenghtOfFile = content.length
                val isStream = url.openStream()
                val testDirectory = File("$filesDir/XML")
                if(!testDirectory.exists()){
                    testDirectory.mkdir()
                }
                val fos = FileOutputStream("$testDirectory/$name.xml")
                val data = ByteArray(1024)
                var count = 0
                var total:Long = 0
                var progress = 0
                count = isStream.read(data)
                while(count != -1){
                    total += count.toLong()
                    val progress_tmp = total.toInt()*100/lenghtOfFile
                    if(progress_tmp%10 == 0 && progress != progress_tmp){
                        progress = progress_tmp
                        publishProgress(progress)
                    }
                    fos.write(data, 0, count)
                    count = isStream.read(data)
                }
                isStream.close()
                fos.close()
            }catch (e: MalformedURLException){
                //run{progressDialog.dismiss()}
                return "Zły URL"
            }catch (e: FileNotFoundException){
                //run{progressDialog.dismiss()}
                return "Brak pilku"
            }catch (e: IOException){
                //run{progressDialog.dismiss()}
                return "wyjątek IO"
            }
            while(!saveRetry(name, expansion)){
                if(!expired){
                    sleepyHead()
                }
                else{
                    runOnUiThread(Runnable() {
                        run() {
                            Toast.makeText(this@MainActivity, "Error", Toast.LENGTH_LONG)
                        }
                    })
                    sleepyHead()
                    finish()
                }
            }
            return "success"
        }

    }

    var userName: String = "XXX"
    var lastSync: String = ""
    var games: Int = 0
    var addins: Int = 0

    lateinit var userButton: Button
    lateinit var syncButton: Button
    lateinit var addsButton: Button
    lateinit var gamesButton: Button
    lateinit var gamesView: TextView
    lateinit var syncView: TextView
    lateinit var addsView: TextView

    var isSavingFinished: Boolean = false
    var isSavingAddinsFinished: Boolean = false
    var isDownloadingFinished: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userButton = findViewById(R.id.User)
        syncButton = findViewById(R.id.Sync)
        gamesButton = findViewById(R.id.Games)
        addsButton = findViewById(R.id.Adds)
        gamesView = findViewById(R.id.gamesNumberView)
        syncView = findViewById(R.id.lastSyncView)
        addsView = findViewById(R.id.addinsNumberView)

        checkLogIn()
        setTexts()
    }

//https://www.geeksforgeeks.org/how-to-check-internet-connection-in-kotlin/
    private fun checkForInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    fun setTexts(){
        val filename =  "$filesDir/data.txt"
        val file = File(filename)
        if(file.exists())
        {
            var datas: List<String> = File(filename).bufferedReader().readLines()
            userName = datas[0]
            lastSync = datas[1]
            games = Integer.parseInt(datas[2])
            addins = Integer.parseInt(datas[3])
        }
        userButton.text = "Hello $userName!"
        gamesView.text = "Number of owned games:  $games"
        addsView.text = "Number of owned add-ins:  $addins"
        syncView.text = "Last synchronized:  $lastSync"
    }

    fun checkLogIn(){
        if(checkForInternet(this)){
            val filename =  "$filesDir/data.txt"
            val file = File(filename)
            if(file.exists())
            {
                var datas: List<String> = File(filename).bufferedReader().readLines()
                userName = datas[0]
                lastSync = datas[1]
                games = Integer.parseInt(datas[2])
                addins = Integer.parseInt(datas[3])
            }
            else{
                showConfiguration()
            }
        }
        else{
            Toast.makeText(this, "This app requires internet connection.\n" +
                    "Try again.", Toast.LENGTH_LONG).show()
        }
    }

    fun gamesClicked(v: View) { showGameActivity() }
    fun showGameActivity() {
        Toast.makeText(this, "content loading", Toast.LENGTH_LONG)
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("addins", false)
        startActivity(i)
    }

    fun dateDiff(a: String, b: String): Long {
        var spt = a.split('-')
        val a_year = (spt[0].toLong())
        val a_month = (spt[1].toLong())
        val a_day = (spt[2].toLong())
        spt = b.split('-')
        val b_year = (spt[0].toLong())
        val b_month = (spt[1].toLong())
        val b_day = (spt[2].toLong())
        val year_diff = b_year - a_year
        val month_diff = b_month - a_month
        val day_diff = b_day - a_day
        val diff = year_diff * 365 + month_diff * 30 + day_diff
        return diff
    }
    fun syncClicked(v: View) {
        val today = SimpleDateFormat("dd-MM-yyyy").format(Date()).toString()
        if(dateDiff(today, lastSync) == 0L) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setMessage("Data is up to date.\nAre you sure you want to synchronize?")
                .setCancelable(false).setPositiveButton("Yes") { dialog, id ->
                    dialog.dismiss()
                    showSync()
            }   .setNegativeButton("No") { dialog, id ->
                    dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        } else {
            showSync()
        }
    }

    val loading = LoadingDialog(this)

    fun showSync() {
        if(loading.startLoading() == 1) sync()
    }

    fun sync() {
        val dbHandler = MyDBHandler(this, null, null,  1)
        dbHandler.clear()
        val q = "https://boardgamegeek.com/xmlapi2/collection?username=$userName&stats=1&excludesubtype=boardgameexpansion"
        downloadFile(q, userName+"_collection", 0)
        val p = "https://boardgamegeek.com/xmlapi2/collection?username=$userName&stats=1&subtype=boardgameexpansion"
        downloadFile(p, userName+"_collection", 1)
    }

    fun downloadFile(link: String, filename: String, exp: Int) {
        isDownloadingFinished = false
        val cd = DataDownloader()
        cd.setData(link, filename, exp)
        cd.execute()
    }

    suspend fun sleep(): Int {
        delay(4000L) // pretend we are doing something useful here
        return 1
    }

    fun sleepyHead() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async { sleep() }
            one.await()
        }
    }

    var expired: Boolean = false
    private fun saveRetry(f: String, expansion: Int):Boolean {
        val testDirectory = File("$filesDir/XML")
        val filename =  "$testDirectory/$f.xml"
        val file = File(filename)
        var xmlDoc: Document

        xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

        val items: NodeList = xmlDoc.getElementsByTagName("item")
        if(items.length == 0) {
            Files.deleteIfExists(Paths.get(filename))
            return false
        }else{
            saveDataMain(items, expansion)
            Files.deleteIfExists(Paths.get(filename))
            return true
        }
    }

    fun saveDataMain(items: NodeList, _expansion: Int) {

        val dbHandler = MyDBHandler(this, null, null,  1)
        for(i in 0..items.length-1){
            val itemNode: Node = items.item(i)
            if(itemNode.nodeType == Node.ELEMENT_NODE) {
                val elem = itemNode as Element
                val children = elem.childNodes
                var id: String? = null
                var title: String? = null
                var year_pub: String? = null
                var pic: String? = null
                var full_pic: String? = null
                val tags = itemNode.attributes
                for(j in 0..tags.length-1){
                    val node = tags.item(j)
                    when (node.nodeName){
                        "objectid" -> {id = node.nodeValue}
                    }
                }
                for(j in 0..children.length-1) {
                    val node = children.item(j)
                    if (node is Element) {
                        when (node.nodeName) {
                            "name" -> {
                                title = node.textContent
                            }
                            "yearpublished" -> {
                                year_pub = node.textContent
                            }
                            "thumbnail" -> {
                                pic = node.textContent
                            }
                            "image" -> {
                                full_pic = node.textContent
                            }
                        }
                    }
                }
                var _id: Int = Integer.parseInt(id)
                var _title: String? = title
                var _org_title: String? = title
                var _year_pub: Int = 0
                if(year_pub == null){
                    _year_pub = 0
                }
                else{
                    _year_pub = Integer.parseInt(year_pub)
                }
                var _pic: String? = pic
                var _full_pic: String? = full_pic

                val product = Record(_id, _title, _org_title, _year_pub, _pic, _full_pic, _expansion)
                dbHandler.addRecord(product)
            }
        }
        isSavingFinished = true
        if(_expansion == 1) isSavingAddinsFinished = true
    }

    fun afterSync() {
        val dbHandler = MyDBHandler(this, null, null,  1)
        games = dbHandler.countGames()
        addins = dbHandler.countAddons()
        lastSync = SimpleDateFormat("dd-MM-yyyy").format(Date()).toString()
        val filename =  "$filesDir/data.txt"
        val file = File(filename)
        file.bufferedWriter().use { out ->
            out.write("$userName\n")
            out.write("$lastSync\n")
            out.write("$games\n")
            out.write("$addins\n")
        }
        setTexts()
    }

    fun userClicked(v: View) { showConfiguration() }
    fun showConfiguration() {
        val popupView: View = layoutInflater.inflate(R.layout.configuration, null)
        val userNameField: EditText = popupView.findViewById(R.id.editUsername)
        val accept: Button = popupView.findViewById(R.id.okButton)
        val dialogBuilder = AlertDialog.Builder(this).setView(popupView)
        val dialog: AlertDialog = dialogBuilder.create()
        dialog.show()
        accept.setOnClickListener() {
            if (userNameField.text.toString() == "") {
                Toast.makeText(this, "Empty username", Toast.LENGTH_SHORT).show()
            }else{
                userName = userNameField.text.toString()
                userButton.text = "Hello $userName!"
                dialog.dismiss()
                showSync()
            }
        }
    }

    fun addinsClicked(v: View) { showAddinsActivity() }
    fun showAddinsActivity() {
        val i = Intent(this, GameActivity::class.java)
        i.putExtra("addins", true)
        startActivity(i)
    }

    fun logOutClicked(v: View) {
        var path = Paths.get("$filesDir/data.txt")
        try {
            val result = Files.deleteIfExists(path)
            if (result) {
                println("Deletion succeeded.")
            } else {
                println("Deletion failed.")
            }
        } catch (e: IOException) {
            println("Deletion failed.")
            e.printStackTrace()
        }
        val dbHandler = MyDBHandler(this, null, null,  1)
        dbHandler.clear()
        val dbPic = DBPictures(this, null, null, 1)
        dbPic.clear()
        finish()
    }
}