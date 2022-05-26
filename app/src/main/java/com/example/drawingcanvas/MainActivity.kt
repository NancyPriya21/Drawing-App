package com.example.drawingcanvas


import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import java.io.BufferedOutputStream

//Inside a LayerList you can have multiple lists, and each layer is an item we create
/*If you want multiple layers on top of each other, we can use FrameLayout as our viewGroup.
 So we are putting our drawingView inside the FrameLayout along with an image View to add a background image */

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView?= null
    private var mCurrentPaint: ImageButton?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView=findViewById(R.id.drawing_view)
        drawingView?.setBrushThickness(20f)

        //using linear layout as an array. Importing view.get
        val linearLayoutColors= findViewById<LinearLayout>(R.id.color_paints)
        mCurrentPaint = linearLayoutColors[0] as ImageButton
        mCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        val brushBtn =findViewById<ImageButton>(R.id.ib_brush)
        brushBtn.setOnClickListener{
            showBrushSizeDialog()
        }

        val ibGalleryAccess= findViewById<ImageButton>(R.id.ib_gallery)
        ibGalleryAccess.setOnClickListener{
            requestStoragePermission()
        }

        val undoBtn= findViewById<ImageButton>(R.id.ib_undo)
        undoBtn.setOnClickListener{
            drawingView?.removeDrawPath()
        }
        val redoBtn= findViewById<ImageButton>(R.id.ib_redo)
        redoBtn.setOnClickListener{
            drawingView?.redoDrawPath()
        }
        val clearBtn= findViewById<ImageButton>(R.id.clear_btn)
            clearBtn.setOnClickListener{
             drawingView?.clearScreen()
        }

    }

    private fun showBrushSizeDialog(){
        val brushDialog = Dialog(this)  //Creates a dialog window that uses the default dialog theme.
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn= brushDialog.findViewById<ImageButton>(R.id.ib_small)
        smallBtn.setOnClickListener{
            drawingView?.setBrushThickness(10f)
            brushDialog.dismiss()
        }
        val mediumBtn= brushDialog.findViewById<ImageButton>(R.id.medium)
        mediumBtn.setOnClickListener{
            drawingView?.setBrushThickness(20f)
            brushDialog.dismiss()
        }
        val largeBtn= brushDialog.findViewById<ImageButton>(R.id.large)
        largeBtn.setOnClickListener{
            drawingView?.setBrushThickness(30f)
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view!=mCurrentPaint){
            val imageBtn = view as ImageButton
            val colorTag = imageBtn.tag.toString()   //tag indicates the color selected by user
            drawingView?.setColor(colorTag)

            imageBtn.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            mCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            mCurrentPaint=view
        }
    }

    //ActivityCompat: Helper for accessing features in android.app.Activity.
    private fun requestStoragePermission(){
        //in case request is denied
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)){
          showRationaleDialog("Kids Drawing App","App needs to Access Your External Storage to set background")
        }
        else{
            requestPermission.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showRationaleDialog(title: String, message:String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title).setMessage(message).setPositiveButton("Cancel"){ dialog, _->
            dialog.dismiss()
        }
        builder.create().show()
    }

    /*ActivityResultLauncher: A launcher for a previously-prepared call to start the
     process of executing an ActivityResultContract.*/
    private val requestPermission :ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                permissions ->
            permissions.entries.forEach {
                val permissionsName =it.key
                val isGranted=it.value

                if(isGranted){
                    Toast.makeText(this, "You can read the storage files", Toast.LENGTH_LONG).show()
                    val pickIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionsName== Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this, "Oops you denied permission",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    //URI: Uniform Resource Identifier - Location on your android device. it is the names of all resources Connected to the World Wide Web.
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground= findViewById<ImageView>(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data) //getting the location of image
            }
        }

    private fun getBitmapFromView(view: View): Bitmap{
        //Define a bitmap with the same size as the view.
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888 )
        val canvas = Canvas(returnedBitmap)   //Binding a canvas to it
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)    //draw the view on the canvas
        return returnedBitmap
    }

    //saving image in device
    private fun addImageToGallery(fileName: String, context: Context, bitmap: Bitmap) {
        val values = ContentValues()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.ImageColumns.TITLE, fileName)

        val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)

        uri?.let {
            context.contentResolver.openOutputStream(uri)?.let { stream ->
                val oStream = BufferedOutputStream(stream)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, oStream)
                oStream.close()
            }
        }
        Toast.makeText(this, "File successfully saved", Toast.LENGTH_LONG).show()
    }

    private fun shareImage(mBitmap: Bitmap){
        val path: String = MediaStore.Images.Media.insertImage(contentResolver, mBitmap,
            "Image Description", null)
        val uri = Uri.parse(path)

        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/jpeg"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(share, "Share Image"))
    }

    //adding buttons on action bar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // R.menu.my_menu is a reference to an xml file named my_menu.xml which should be inside your res/menu directory.
        // If you don't have res/menu, just create a directory named "menu" inside res
        menuInflater.inflate(R.menu.mymenu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // handle button activities on action bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.saveBtn) {
            if(isReadStorageAllowed()){
                val flDrawingView =findViewById<FrameLayout>(R.id.fl_drawing_view_container)
                val myBitmap= getBitmapFromView(flDrawingView)
                addImageToGallery("DrawingApp"+System.currentTimeMillis()/1000, this, myBitmap)
            }
        }
        if (id == R.id.shareBtn) {
            val flDrawingView =findViewById<FrameLayout>(R.id.fl_drawing_view_container)
            shareImage(getBitmapFromView(flDrawingView))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        //PackageManager.PERMISSION_GRANTED has final int as 0 in PackageManager
        if(result==PackageManager.PERMISSION_GRANTED)
            return true
        return false
    }

 }



