package com.example.favdish.view.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.favdish.application.FavDishApplication
import com.example.favdish.model.entities.FavDish
import com.example.favdish.utils.Constants
import com.example.favdish.utils.TakePictureWithUriReturnContract
import com.example.favdish.view.activities.databinding.ActivityAddUpdateDishBinding
import com.example.favdish.view.activities.databinding.DialogCustomImageSelectionBinding
import com.example.favdish.view.activities.databinding.DialogCustomListBinding
import com.example.favdish.view.adapters.CustomListItemAdapter
import com.example.favdish.viewmodel.FavDishViewModel
import com.example.favdish.viewmodel.FavDishViewModelFactory
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*


class AddUpdateDishActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityAddUpdateDishBinding
    private var mImagePath: String = "" //we will override it when we've selected an image
    private lateinit var mCustomListDialog: Dialog //so we can use customItemsListDialog everywhere
    private val mFavDishViewModel: FavDishViewModel by viewModels {
        FavDishViewModelFactory((application as FavDishApplication).repository)
    }
    private var mFavDishDetails: FavDish? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddUpdateDishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Check if intent has extra from constant EXTRADISHDETAILS
        if (intent.hasExtra(Constants.EXTRA_DISH_DETAILS)){
            //if yes, put it in the global var, which will be of type FavDish
            mFavDishDetails = intent.getParcelableExtra(Constants.EXTRA_DISH_DETAILS)
        }

        setupActionBar()

        //check if mFavDishDetails is not empty, and change image and text displayed
        mFavDishDetails?.let {
            if (it.id != 0){
                mImagePath =it.image
                Glide.with(this@AddUpdateDishActivity)
                    .load(mImagePath)
                    .centerCrop()
                    .into(binding.ivDishImage)

                binding.etTitle.setText(it.title)
                binding.etType.setText(it.type)
                binding.etCategory.setText(it.category)
                binding.etIngredients.setText(it.ingredients)
                binding.etCookingTimeInMinutes.setText(it.cookingTime)
                binding.etDirectionToCook.setText(it.directionToCook)

                binding.btnAddDish.text = resources.getString(R.string.lbl_update_dish)
            }
        }

        //adding onClickListener so the element is clickable
        binding.ivAddDishImage.setOnClickListener(this)
        binding.etCategory.setOnClickListener(this)
        binding.etType.setOnClickListener(this)
        binding.etCookingTimeInMinutes.setOnClickListener(this)
        binding.btnAddDish.setOnClickListener(this)

    }

    //setup the navigation bar on top of the screen
    private fun setupActionBar() {
        setSupportActionBar(binding.toolbarAddDishActivity)
        //check if global var is not null has an id
        if (mFavDishDetails !=null && mFavDishDetails!!.id != 0){
            //if yes, change the title in the action bar
            supportActionBar?.let {
                it.title = resources.getString(R.string.title_edit_dish)
            }
            //if not, we just adding a new dish, like usual
        }else{
            supportActionBar?.let {
                it.title = resources.getString(R.string.title_edit_dish)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAddDishActivity.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    //precising what function should be launch when clicking specified element
    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.iv_add_dish_image -> {
                    customImageSelectionDialog()
                    return
                }
                R.id.et_category -> {
                    customItemsListDialog(
                        resources.getString(R.string.title_select_dish_category),
                        Constants.dishCategories(),
                        Constants.DISH_CATEGORY
                    )
                    return
                }
                R.id.et_type -> {
                    customItemsListDialog(
                        resources.getString(R.string.title_select_dish_type),
                        Constants.dishTypes(),
                        Constants.DISH_TYPE
                    )
                    return
                }
                R.id.et_cooking_time_in_minutes -> {
                    customItemsListDialog(
                        resources.getString(R.string.title_select_dish_cooking_time),
                        Constants.dishCookingTime(),
                        Constants.DISH_COOKING_TIME
                    )
                    return
                }
                R.id.btn_add_dish -> {
                    val title = binding.etTitle.text.toString()
                        .trim { it <= ' ' }//this line trim empty spaces
                    val type = binding.etType.text.toString().trim { it <= ' ' }
                    val category = binding.etCategory.text.toString().trim { it <= ' ' }
                    val ingredients = binding.etIngredients.text.toString().trim { it <= ' ' }
                    val cookingTimeInMinutes =
                        binding.etCookingTimeInMinutes.text.toString().trim { it <= ' ' }
                    val cookingDirection =
                        binding.etDirectionToCook.text.toString().trim { it <= ' ' }

                    //here we check if those editText are empty, and show a toast
                    when {
                        TextUtils.isEmpty(mImagePath) -> { //put the container that's supposed to be empty in ()
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_image),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        TextUtils.isEmpty(title) -> {
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_title),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        TextUtils.isEmpty(type) -> {
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_type),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        TextUtils.isEmpty(category) -> {
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_category),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        TextUtils.isEmpty(ingredients) -> {
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_ingredients),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        TextUtils.isEmpty(cookingTimeInMinutes) -> {
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_cooking_time),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        TextUtils.isEmpty(cookingDirection) -> {
                            Toast.makeText(
                                this@AddUpdateDishActivity,
                                resources.getString(R.string.err_msg_select_dish_cooking_instructions),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            var dishID = 0
                            var imageSource = Constants.DISH_IMAGE_SOURCE_LOCAL
                            var favoriteDish = false

                            //if mFavDishDetails is not null, we check if it's not new entry(id=0)
                            //and we set its id, image and favorite value
                            mFavDishDetails?.let {
                                if (it.id != 0){
                                    dishID = it.id
                                    imageSource= it.imagesource
                                    favoriteDish = it.favoriteDish
                                }
                            }

                            //we create the val with all the info
                            val favDishDetails: FavDish = FavDish(
                                mImagePath,
                                imageSource,
                                title,
                                type,
                                category,
                                ingredients,
                                cookingTimeInMinutes,
                                cookingDirection,
                                favoriteDish,
                                dishID
                            )

                            //check if it's a new dish, then insert the val with all the info into room-database
                            if (dishID == 0){
                                mFavDishViewModel.insert(favDishDetails)
                                Toast.makeText(
                                    this@AddUpdateDishActivity,
                                    "Favorite dish added",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.i("insertion", "Success")
                            //or else we update the info
                            }else{
                                mFavDishViewModel.update(favDishDetails)
                                Toast.makeText(
                                    this@AddUpdateDishActivity,
                                    "Favorite dish updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.i("updating", "Success")
                            }
                            finish()
                        }
                    }
                }
            }
        }
    }

    //PB: black screen when the dialog popup
    private fun customImageSelectionDialog() {
        val dialog = Dialog(this)
        val dialbinding: DialogCustomImageSelectionBinding =
            DialogCustomImageSelectionBinding.inflate(layoutInflater)
        dialog.setContentView(dialbinding.root)

        dialbinding.tvCamera.setOnClickListener {
            Dexter.withContext(this).withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()) {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            takePicture.launch(intent)
                            //startActivityForResult(intent, CAMERA)
                            /*getTmpFileUri().let { uri ->

                                takePictureResult.launch(uri)

                            }*/
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermission()
                }

            }
            ).onSameThread().check()
            dialog.dismiss()

        }

        dialbinding.tvGallery.setOnClickListener {

            Dexter.withContext(this@AddUpdateDishActivity).withPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    val intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    pickImage.launch(intent)
                    //pickImageResult.launch("image/*")
                    //startActivityForResult(galleryIntent, GALLERY)
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        this@AddUpdateDishActivity,
                        "you have denied storage permission", Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    showRationalDialogForPermission()
                }

            }).onSameThread().check()
            dialog.dismiss()

        }
        dialog.show()
    }

    //used in the onClickListener of the adapter, for we click on an item of the list
    fun selectedListItem(item: String, selection: String) {
        when (selection) {
            Constants.DISH_TYPE -> {
                mCustomListDialog.dismiss()
                binding.etType.setText(item)
            }
            Constants.DISH_CATEGORY -> {
                mCustomListDialog.dismiss()
                binding.etCategory.setText(item)
            }
            else -> {
                mCustomListDialog.dismiss()
                binding.etCookingTimeInMinutes.setText(item)
            }
        }
    }

    //pick image from gallery and set it as thumbnail
    val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { //
                    val selectedPhotoUri = result.data!!.data
                    Glide.with(this)
                        .load(selectedPhotoUri)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL) //check all the cache for existing picture
                        .listener(object :
                            RequestListener<Drawable> { //used for tracking error during loading
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.e("TAG", "Error loading image", e)
                                return false //use return false to use placeholder
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                resource?.let {
                                    val bitmap: Bitmap =
                                        resource.toBitmap() // convert the resource in Bitmap and save in bitmap
                                    //take the string of bitmap, give it an ID and place in a folder, and save that path in mImagePath
                                    mImagePath = saveImageToInternalStorage(bitmap)
                                    Log.i("ImagePath", mImagePath)
                                }
                                return false
                            }
                        })
                        .into(binding.ivDishImage)

                    //we change the icon to take a picture into the edit icon
                    binding.ivAddDishImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_vector_edit
                        )
                    )
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.e("cancelled", "User cancelled image selection")
            }
        }

    //take picture with camera and set it as thumbnail
    val takePicture =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.extras?.let { //data.extra is the image
                    // get it, convert in bitmap, save it
                    val thumbnail: Bitmap = result.data!!.extras!!.get("data") as Bitmap
                    //we use glide to modify and set the image where we want, as a thumbnail
                    Glide.with(this)
                        .load(thumbnail)
                        .centerCrop()
                        .into(binding.ivDishImage)

                    //we take the string of the thumbnail, give it an ID and place in a folder, and save that path in mImagePath
                    mImagePath = saveImageToInternalStorage(thumbnail)
                    Log.i("image path", mImagePath)

                    //we change the icon to take a picture into the edit icon
                    binding.ivAddDishImage.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_vector_edit
                        )
                    )
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.e("cancelled", "User cancelled taking picture")
            }
        }

    private val takePictureResult =
        registerForActivityResult(TakePictureWithUriReturnContract()) { (isSuccess, imageUri) ->
            if (isSuccess) {

                binding.ivDishImage.setImageURI(imageUri)

            }
        }

    private var latestTmpUri: Uri? = null

    private val pickImageResult =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { binding.ivDishImage.setImageURI(uri) }
        }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            tmpFile
        )
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){//if u went back to and from camera without pb then
            if (requestCode == CAMERA){//if we talking about the camera then
                data?.extras?.let { //data.extra is the image, get it convert in bitmap set it as thumbnail
                    val thumbnail : Bitmap = data.extras!!.get("data") as Bitmap

                    Glide.with(this)
                        .load(thumbnail)
                        .centerCrop()
                        .into(binding.ivDishImage)

                    binding.ivAddDishImage.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_vector_edit))
                }
            }
            if (requestCode == GALLERY){//if we talking about the camera then
                data?.let { //data.extra is the image, get it convert in bitmap set it as thumbnail
                    val selectedPhotoUri = data.data
                    //binding.ivDishImage.setImageURI(selectedPhotoUri)
                    Glide.with(this)
                        .load(selectedPhotoUri)
                        .centerCrop()
                        .into(binding.ivDishImage)

                    binding.ivAddDishImage.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.ic_vector_edit))
                }
            }
        }else if (resultCode ==Activity.RESULT_CANCELED){
            Log.e("cancelled", "User cancelled image selection")
        }

    }*/

    private fun showRationalDialogForPermission() {
        AlertDialog.Builder(this)
            .setMessage("you turned off permission, enable it in application settings")
            .setPositiveButton("GO TO SETTINGS")
            { _, _ ->
                try {
                    val intent =
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)//send you to your phone settings
                    val uri = Uri.fromParts("package", packageName, null)//address of our app
                    intent.data = uri //intent will use data (=uri) to send you somewhere
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    //This will take bitmap and return the absolute path of the file we have stored
    private fun saveImageToInternalStorage(bitmap: Bitmap): String {
        val wrapper = ContextWrapper(applicationContext)

        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(
            file,
            "${UUID.randomUUID()}.jpg"
        ) //File need folder and name as parameter, UUID give random unique ID

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    //set up of the adapter for the dialog
    private fun customItemsListDialog(title: String, itemsList: List<String>, selection: String) {
        mCustomListDialog = Dialog(this)
        val binding: DialogCustomListBinding = DialogCustomListBinding.inflate(layoutInflater)
        mCustomListDialog.setContentView(binding.root)

        //the textview will take the title we entered in the parameter
        binding.tvTitle.text = title

        //we set up the adapter
        binding.rvList.layoutManager = LinearLayoutManager(this)
        val adapter = CustomListItemAdapter(this, null, itemsList, selection)
        binding.rvList.adapter = adapter

        mCustomListDialog.show()

    }

    companion object {
        private const val CAMERA = 1
        private const val GALLERY = 2

        //where our image are stored
        private const val IMAGE_DIRECTORY = "FavDishImage"

    }
}