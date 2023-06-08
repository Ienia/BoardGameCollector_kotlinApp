import android.app.Activity
import android.app.AlertDialog
import edu.put.inf151904.R

class LoadingDialog(val mActivity: Activity) {
    private lateinit var isdialog: AlertDialog
    fun startLoading(): Int {
        //set view
        val inflater = mActivity.layoutInflater
        val dialogView = inflater.inflate(R.layout.synchronization, null)
        //set dialog
        val builder = AlertDialog.Builder(mActivity)
        builder.setView(dialogView)
        builder.setCancelable(false)
        isdialog = builder.create()
        isdialog.show()
        return 1
    }
    fun isDismiss() {
        isdialog.dismiss()
    }
}