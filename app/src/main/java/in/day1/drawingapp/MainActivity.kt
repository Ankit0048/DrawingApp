package `in`.day1.drawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

//    variable set the view xml file
    private var drawingView: DrawingView? = null
    private var igBrush: ImageButton? = null
    private var mImageButtonCurrentPaint : ImageButton?= null
    private var igallery: ImageButton?= null
    private var ibUndo: ImageButton?= null
    private var ibRedo: ImageButton?= null
    private var ibSave: ImageButton?= null
    private var customDialog: Dialog?= null

    //Set the request permission variable to do when the request permission is received sets it to a
//    variable
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
                permissions.entries.forEach {
                    val permissionName= it.key
                    val isGranted = it.value
                    if (isGranted) {
                        if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE.toString()) {
                            Toast.makeText(
                                this, "Permission granted now you can read the files",
                                Toast.LENGTH_SHORT
                            ).show()

//                        Create an intent of the local device gallery to select images from it
                            val pickIntent = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            openGalleryLauncher.launch(pickIntent)
                        }
                        else {
                            Toast.makeText(
                                this, "Permission granted now you can write the files",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    else {
                        if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                            Toast.makeText(this, "Permission of Storage not granted",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }


//    Initiate the new intent of the gallery to pick the image
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null) {
                val imageBackground : ImageView = findViewById(R.id.tv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        we get the view from the xml file where we call this view
        drawingView = findViewById(R.id.drawing_view)
//        Get the linear layout of the palet colors here
        val linearLayoutPaint : LinearLayout = findViewById(R.id.ll_paintcolors)
        mImageButtonCurrentPaint = linearLayoutPaint[3] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected))
//        The first selected color is then set as pressed

        drawingView?.setBrushSize(20.toFloat())
        igBrush = findViewById(R.id.ib_brush)
        igBrush?.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        igallery = findViewById(R.id.img_import)
        igallery?.setOnClickListener{
            requestStoragePermission()
        }

//        Setting the call for undo button
        ibUndo = findViewById(R.id.ib_undo)
        ibUndo?.setOnClickListener{
            drawingView?.undoDraw()
        }

//        Setting the call for redo button
        ibRedo = findViewById(R.id.ib_redo)
        ibRedo?.setOnClickListener{
            drawingView?.redoDraw()
        }

        ibSave = findViewById(R.id.ib_export)
        ibSave?.setOnClickListener{
            if(isReadallowed()) {
                showCustomProgressDialog()
                lifecycleScope.launch{
                    val flDrawingview : FrameLayout = findViewById(R.id.fl_drawing_view_container)
//                  save the bitmap image from the frame Layout
                    saveBitmapFile(getBitmapFromView(flDrawingview))
                }
                cancelCustomDialog()
            }
        }
    }

    private fun showBrushSizeChooserDialog() {
//        opens the dialog to set the brush sizes
        var brushDialog = Dialog(this)
//        Attach the layout of the size section view
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        brushDialog.show()
//        Attach buttons so that the we call the desired functions
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setBrushSize(30.toFloat())
            brushDialog.dismiss()
        }

    }

    fun paintClicked(view: View) {
//        To select that which paint is clicked from the palette
        if(view!==mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
//            Convert the tag to the string
            val colortag = imageButton.tag.toString()
            drawingView?.setColor(colortag)
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal))
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected))
            mImageButtonCurrentPaint = imageButton
        }

    }

//    Create a custom Dialog which can we gives a string to show as a text
    private fun showRationaleDialog(title: String, message: String) {
        val builder : AlertDialog.Builder= AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

//    function to check whether we have the read permission available or not
    private fun isReadallowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(this,
        Manifest. permission.WRITE_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

//    Function to ask for the permission of the storage
    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)) {
            showRationaleDialog("Kids Drawing App", "Kids Drawing App needs to ACCESS your Storage")
        }
        else {
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

//    Function to get bitmap from the view of the canvas
    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        }
        else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

//
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if(mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator
                    + "KidsDrawing_" + "${System.currentTimeMillis()/100}" + ".png")

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        if(!result.isEmpty()) {
                            Toast.makeText(this@MainActivity, "File Saved Successfully",
                                Toast.LENGTH_SHORT).show()
                            sharefile(result)
                        }
                        else {
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }

        }
        return result
    }

//    Show the custom progress Dialog for the saving
    private fun showCustomProgressDialog() {
        customDialog = Dialog(this)
        customDialog?.setContentView(R.layout.progressdialog)
        customDialog?.show()
    }

//    cancel the appearance of the dialog
    private fun cancelCustomDialog() {
        if(customDialog != null) {
            customDialog?.dismiss()
            customDialog = null
        }
    }

//    Function to share the image via email
    private fun sharefile(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) {
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}