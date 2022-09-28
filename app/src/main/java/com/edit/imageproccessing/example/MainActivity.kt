package com.edit.imageproccessing.example

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.edit.imageproccessing.OnProcessingCompletionListener
import com.edit.imageproccessing.PhotoFilter
import com.edit.imageproccessing.filters.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnProcessingCompletionListener, OnFilterClickListener {
    private val REQUEST_PERMISSION: Int = 10001
    private lateinit var result: Bitmap
    private var photoFilter: PhotoFilter? = null
    lateinit var currentPhotoPath: String
    var imagePath: String = ""
    lateinit var filterImage: Bitmap
    private var isCamera = false


/*    private var cameraResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { result: Instrumentation.ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            imagePath = currentPhotoPath
           *//* ivReceipt.setImageURI(imagePath.toUri())*//*
        }
    }*/

    /* private fun moveToCamera() {
         val photoFile: File? = try {
             createImageFile()
         } catch (ex: IOException) {
             // Error occurred while creating the File
             null
         }
         val photoURI: Uri = FileProvider.getUriForFile(
             this,
             "${BuildConfig.APPLICATION_ID}.provider",
             photoFile!!
         )
         isCamera = true
         val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
         cameraResultLauncher.launch(takePictureIntent)
     }
 */
    override fun onProcessingComplete(bitmap: Bitmap) {
        // Do anything with the bitmap save it or add another effect to it
        result = bitmap
    }

    override fun onFilterClicked(effectsThumbnail: EffectsThumbnail) {
        photoFilter?.applyEffect(filterImage, effectsThumbnail.filter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    private fun initialize() {
        filterImage = BitmapFactory.decodeResource(resources, R.drawable.dog)
        photoFilter = PhotoFilter(effectView, this)
        photoFilter?.applyEffect(filterImage, None())
        effectsRecyclerView.layoutManager =
            LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
        effectsRecyclerView.setHasFixedSize(true)
        effectsRecyclerView.adapter = EffectsAdapter(getItems(), this@MainActivity)
        saveButton.setOnClickListener {
            checkPermissionAndSaveImage()
        }
        cameraButton.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION)
            } else {
                openCamera()
            }
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        /*  val photoFile: File? = try {
              createImageFile()
          } catch (ex: IOException) {
              null
          }
          val photoURI: Uri =
              FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", photoFile!!)*/
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//         takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(takePictureIntent, REQUEST_PERMISSION)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PERMISSION && data != null) {

            filterImage = data.extras?.get("data") as Bitmap
            photoFilter?.applyEffect(filterImage, None())
        }
    }

    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            Log.e("TAG", "absolutePath: $absolutePath")
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
        Log.e("TAG", "createImageFile: $currentPhotoPath")
        return file
    }

    private fun showImage(uri: Uri) {
        Glide.with(this)
            .asBitmap()
            .centerCrop()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    imagePath = saveToInternalStorage(this@MainActivity, resource)!!
                    photoFilter = PhotoFilter(effectView, this@MainActivity)
                    photoFilter?.applyEffect(
                        BitmapFactory.decodeResource(
                            resources,
                            imagePath.toInt()
                        ), None()
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }

    fun saveToInternalStorage(context: Context, bitmapImage: Bitmap): String? {
        try {
            val directory = context.cacheDir.absolutePath
            val mypath = File(directory + "/" + System.currentTimeMillis() + "profile.jpg")
            var fos: FileOutputStream? = null
            fos = FileOutputStream(mypath)
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            return mypath.absolutePath
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun checkPermissionAndSaveImage() {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION
                )
            } else {
                saveImage()
            }
        } else {
            saveImage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImage()
                } else {
                    Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
                return
            }
        }
    }

    private fun saveImage() {
        val path = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString()
        val fOut: OutputStream?
        val fileName = Date().time
        val file = File(path, "$fileName.jpg")
        fOut = FileOutputStream(file)
        result.compress(Bitmap.CompressFormat.JPEG, 85, fOut)
        fOut.flush()
        fOut.close()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString())
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (VERSION.SDK_INT >= VERSION_CODES.Q) { //this one
                put(MediaStore.MediaColumns.RELATIVE_PATH, path)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        MediaStore.Images.Media.insertImage(
            contentResolver, file.absolutePath, file.name, file.name
        )
        Toast.makeText(this@MainActivity, "ImageSaved", Toast.LENGTH_SHORT).show()
    }

    private fun getItems(): MutableList<EffectsThumbnail> {
        return mutableListOf(
            EffectsThumbnail("None", None()),
            EffectsThumbnail("AutoFix", AutoFix()),
            EffectsThumbnail("Highlight", Highlight()),
            EffectsThumbnail("Brightness", Brightness()),
            EffectsThumbnail("Contrast", Contrast()),
            EffectsThumbnail("Cross Process", CrossProcess()),
            EffectsThumbnail("Documentary", Documentary()),
            EffectsThumbnail("Duo Tone", DuoTone()),
            EffectsThumbnail("Fill Light", FillLight()),
            EffectsThumbnail("Fisheye", FishEye()),
            EffectsThumbnail("Flip Horizontally", FlipHorizontally()),
            EffectsThumbnail("Flip Vertically", FlipVertically()),
            EffectsThumbnail("Grain", Grain()),
            EffectsThumbnail("Grayscale", Grayscale()),
            EffectsThumbnail("Lomoish", Lomoish()),
            EffectsThumbnail("Negative", Negative()),
            EffectsThumbnail("Posterize", Posterize()),
            EffectsThumbnail("Rotate", Rotate()),
            EffectsThumbnail("Saturate", Saturate()),
            EffectsThumbnail("Sepia", Sepia()),
            EffectsThumbnail("Sharpen", Sharpen()),
            EffectsThumbnail("Temperature", Temperature()),
            EffectsThumbnail("Tint", Tint()),
            EffectsThumbnail("Vignette", Vignette())
        )
    }
}