package au.edu.utas.kit305.assignment2

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import au.edu.utas.kit305.assignment2.databinding.ActivityRoomDetailBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemWindowBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemFloorSpaceBinding
import java.io.ByteArrayOutputStream
import java.io.File

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityRoomDetailBinding
    private var houseId: String? = null
    private var roomId: String? = null

    // Lists to hold windows and floor spaces loaded from Firebase
    private val windows = mutableListOf<Window>()
    private val floorSpaces = mutableListOf<FloorSpace>()

    // URI pointing to the temp file the camera will write into
    private var photoUri: Uri? = null

    // Build the camera intent manually so we can add FLAG_GRANT_WRITE_URI_PERMISSION.
    // TakePicture() contract omits this flag, which causes the camera to return
    // RESULT_CANCELED immediately when given a FileProvider URI.
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uploadPhoto(it) }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
    }

    // Gallery: user picks an image, we receive its content URI
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadPhoto(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityRoomDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Get house and room IDs passed from HouseDetailActivity
        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")
        val roomName = intent.getStringExtra("ROOM_NAME")

        // Set toolbar title to room name
        ui.toolbar.title = roomName ?: "Room"
        ui.toolbar.setNavigationOnClickListener { finish() }

        // Set up RecyclerViews with their adapters
        ui.windowList.layoutManager = LinearLayoutManager(this)
        ui.windowList.adapter = WindowAdapter(windows)

        ui.floorSpaceList.layoutManager = LinearLayoutManager(this)
        ui.floorSpaceList.adapter = FloorSpaceAdapter(floorSpaces)

        // Attach swipe-to-delete for both lists
        setupSwipeToDelete(ui.windowList, isWindow = true)
        setupSwipeToDelete(ui.floorSpaceList, isWindow = false)

        // Show hint to user about swipe and tap features
        com.google.android.material.snackbar.Snackbar.make(
            ui.root,
            "💡 Tap to edit, swipe left to delete",
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show()

        // Delete room button with confirmation dialog
        ui.btnDeleteRoom.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Room")
                .setMessage("Are you sure? All windows and floor spaces will be lost!")
                .setPositiveButton("Delete") { _, _ -> deleteRoom() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Add window button opens AddWindowActivity in add mode
        ui.btnAddWindow.setOnClickListener {
            val intent = Intent(this, AddWindowActivity::class.java)
            intent.putExtra("HOUSE_ID", houseId)
            intent.putExtra("ROOM_ID", roomId)
            startActivity(intent)
        }

        // Add floor space button opens AddFloorSpaceActivity in add mode
        ui.btnAddFloorSpace.setOnClickListener {
            val intent = Intent(this, AddFloorSpaceActivity::class.java)
            intent.putExtra("HOUSE_ID", houseId)
            intent.putExtra("ROOM_ID", roomId)
            startActivity(intent)
        }

        // Add photo button shows camera/gallery choice
        ui.btnAddPhoto.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Add Room Photo")
                .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                    if (which == 0) checkCameraPermissionAndLaunch() else launchGallery()
                }
                .show()
        }

        // Load data from Firebase
        loadWindows()
        loadFloorSpaces()
        loadRoomPhoto()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Create the output file in external app storage, wrap with FileProvider, then launch
    // the camera intent with explicit write permission so the camera app can save the photo.
    private fun launchCamera() {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: cacheDir
        val photoFile = File.createTempFile("room_photo_", ".jpg", storageDir)
        photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        cameraLauncher.launch(intent)
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    // Display from the local URI immediately, then compress + upload to Firebase Storage.
    // Showing first means the photo always appears even if the upload is slow or fails.
    private fun uploadPhoto(uri: Uri) {
        val hid = houseId ?: return
        val rid = roomId ?: return

        // Show the photo right away from the local file — no waiting for network
        ui.imgRoomPhoto.visibility = View.VISIBLE
        Glide.with(this).load(uri).into(ui.imgRoomPhoto)
        ui.btnAddPhoto.text = "Change Photo"

        Toast.makeText(this, "Saving photo...", Toast.LENGTH_SHORT).show()

        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Toast.makeText(this, "Could not read photo", Toast.LENGTH_SHORT).show()
                return
            }

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()

            FirebaseStorage.getInstance().reference
                .child("houses/$hid/rooms/$rid/photo.jpg")
                .putBytes(bytes)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            Firebase.firestore
                                .collection("houses").document(hid)
                                .collection("rooms").document(rid)
                                .set(
                                    hashMapOf("photoUrl" to downloadUri.toString()),
                                    com.google.firebase.firestore.SetOptions.merge()
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Photo saved!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Firestore save failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Could not get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Read the photoUrl field from Firestore and display with Glide
    private fun loadRoomPhoto() {
        val hid = houseId ?: return
        val rid = roomId ?: return

        Firebase.firestore
            .collection("houses").document(hid)
            .collection("rooms").document(rid)
            .get()
            .addOnSuccessListener { doc ->
                val photoUrl = doc.getString("photoUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    ui.imgRoomPhoto.visibility = View.VISIBLE
                    Glide.with(this).load(photoUrl).into(ui.imgRoomPhoto)
                    ui.btnAddPhoto.text = "Change Photo"
                }
            }
    }

    // Sets up swipe-to-delete with red background for a RecyclerView
    private fun setupSwipeToDelete(recyclerView: RecyclerView, isWindow: Boolean) {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            // Draw red background and delete icon while swiping
            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                paint.color = Color.parseColor("#CC0000")
                c.drawRect(
                    itemView.right + dX, itemView.top.toFloat(),
                    itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                )
                // Draw delete icon on the red background
                val icon = ContextCompat.getDrawable(this@RoomDetailActivity, android.R.drawable.ic_menu_delete)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            // Called when item is fully swiped - show confirmation dialog
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (isWindow) {
                    val window = windows[position]
                    AlertDialog.Builder(this@RoomDetailActivity)
                        .setTitle("Delete Window")
                        .setMessage("Are you sure you want to delete '${window.name}'?")
                        .setPositiveButton("Delete") { _, _ -> deleteWindow(window, position) }
                        .setNegativeButton("Cancel") { _, _ ->
                            // Restore item if user cancels
                            ui.windowList.adapter?.notifyDataSetChanged()
                        }
                        .show()
                } else {
                    val fs = floorSpaces[position]
                    AlertDialog.Builder(this@RoomDetailActivity)
                        .setTitle("Delete Floor Space")
                        .setMessage("Are you sure you want to delete '${fs.name}'?")
                        .setPositiveButton("Delete") { _, _ -> deleteFloorSpace(fs, position) }
                        .setNegativeButton("Cancel") { _, _ ->
                            // Restore item if user cancels
                            ui.floorSpaceList.adapter?.notifyDataSetChanged()
                        }
                        .show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    // Delete a window from Firebase and update the list
    private fun deleteWindow(window: Window, position: Int) {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                window.id?.let { wid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("windows").document(wid)
                        .delete()
                        .addOnSuccessListener {
                            windows.removeAt(position)
                            ui.windowList.adapter?.notifyDataSetChanged()
                            Toast.makeText(this, "Window deleted!", Toast.LENGTH_SHORT).show()
                            // Show empty state if no windows left
                            ui.txtNoWindows.visibility = if (windows.isEmpty()) View.VISIBLE else View.GONE
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error deleting window", Toast.LENGTH_SHORT).show()
                            ui.windowList.adapter?.notifyDataSetChanged()
                        }
                }
            }
        }
    }

    // Delete a floor space from Firebase and update the list
    private fun deleteFloorSpace(fs: FloorSpace, position: Int) {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                fs.id?.let { fid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("floorspaces").document(fid)
                        .delete()
                        .addOnSuccessListener {
                            floorSpaces.removeAt(position)
                            ui.floorSpaceList.adapter?.notifyDataSetChanged()
                            Toast.makeText(this, "Floor space deleted!", Toast.LENGTH_SHORT).show()
                            // Show empty state if no floor spaces left
                            ui.txtNoFloorSpaces.visibility = if (floorSpaces.isEmpty()) View.VISIBLE else View.GONE
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error deleting floor space", Toast.LENGTH_SHORT).show()
                            ui.floorSpaceList.adapter?.notifyDataSetChanged()
                        }
                }
            }
        }
    }

    // Delete entire room from Firebase and go back
    private fun deleteRoom() {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Room deleted!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error deleting room", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Load windows from Firebase subcollection
    private fun loadWindows() {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("windows")
                    .get()
                    .addOnSuccessListener { result ->
                        windows.clear()
                        for (doc in result) {
                            val window = doc.toObject(Window::class.java)
                            window.id = doc.id
                            windows.add(window)
                        }
                        // Show empty state or list
                        ui.txtNoWindows.visibility = if (windows.isEmpty()) View.VISIBLE else View.GONE
                        ui.windowList.adapter?.notifyDataSetChanged()
                    }
            }
        }
    }

    // Load floor spaces from Firebase subcollection
    private fun loadFloorSpaces() {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("floorspaces")
                    .get()
                    .addOnSuccessListener { result ->
                        floorSpaces.clear()
                        for (doc in result) {
                            val fs = doc.toObject(FloorSpace::class.java)
                            fs.id = doc.id
                            floorSpaces.add(fs)
                        }
                        // Show empty state or list
                        ui.txtNoFloorSpaces.visibility = if (floorSpaces.isEmpty()) View.VISIBLE else View.GONE
                        ui.floorSpaceList.adapter?.notifyDataSetChanged()
                    }
            }
        }
    }

    // Reload data every time we come back to this screen
    override fun onResume() {
        super.onResume()
        loadWindows()
        loadFloorSpaces()
        loadRoomPhoto()
    }

    // ViewHolder for window list items
    inner class WindowHolder(var ui: ListItemWindowBinding) : RecyclerView.ViewHolder(ui.root)

    // Adapter for window list
    inner class WindowAdapter(private val list: MutableList<Window>) : RecyclerView.Adapter<WindowHolder>() {
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowHolder {
            val ui = ListItemWindowBinding.inflate(layoutInflater, parent, false)
            return WindowHolder(ui)
        }

        override fun onBindViewHolder(holder: WindowHolder, position: Int) {
            val window = list[position]
            holder.ui.txtWindowName.text = window.name ?: "No name"
            holder.ui.txtWindowSize.text = "${window.width} x ${window.height} mm"

            // Tap window card to open edit screen with pre-filled data
            holder.itemView.setOnClickListener {
                val intent = Intent(this@RoomDetailActivity, AddWindowActivity::class.java)
                intent.putExtra("HOUSE_ID", houseId)
                intent.putExtra("ROOM_ID", roomId)
                intent.putExtra("WINDOW_ID", window.id)
                intent.putExtra("WINDOW_NAME", window.name)
                intent.putExtra("WINDOW_WIDTH", window.width)
                intent.putExtra("WINDOW_HEIGHT", window.height)
                intent.putExtra("WINDOW_PRODUCT", window.productName)
                intent.putExtra("WINDOW_COLOUR", window.productColour)
                intent.putExtra("WINDOW_PRICE", window.estimatedPrice)
                intent.putExtra("WINDOW_PRICE_PER_M2", window.pricePerM2)
                intent.putExtra("WINDOW_LABOUR", window.labourCost)
                startActivity(intent)
            }
        }
    }

    // ViewHolder for floor space list items
    inner class FloorSpaceHolder(var ui: ListItemFloorSpaceBinding) : RecyclerView.ViewHolder(ui.root)

    // Adapter for floor space list
    inner class FloorSpaceAdapter(private val list: MutableList<FloorSpace>) : RecyclerView.Adapter<FloorSpaceHolder>() {
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloorSpaceHolder {
            val ui = ListItemFloorSpaceBinding.inflate(layoutInflater, parent, false)
            return FloorSpaceHolder(ui)
        }

        override fun onBindViewHolder(holder: FloorSpaceHolder, position: Int) {
            val fs = list[position]
            holder.ui.txtFloorSpaceName.text = fs.name ?: "No name"
            holder.ui.txtFloorSpaceSize.text = "${fs.width} x ${fs.depth} m"

            // Tap floor space card to open edit screen with pre-filled data
            holder.itemView.setOnClickListener {
                val intent = Intent(this@RoomDetailActivity, AddFloorSpaceActivity::class.java)
                intent.putExtra("HOUSE_ID", houseId)
                intent.putExtra("ROOM_ID", roomId)
                intent.putExtra("FLOOR_ID", fs.id)
                intent.putExtra("FLOOR_NAME", fs.name)
                intent.putExtra("FLOOR_WIDTH", fs.width)
                intent.putExtra("FLOOR_DEPTH", fs.depth)
                intent.putExtra("FLOOR_PRODUCT", fs.productName)
                intent.putExtra("FLOOR_COLOUR", fs.productColour)
                intent.putExtra("FLOOR_PRICE", fs.estimatedPrice)
                intent.putExtra("FLOOR_PRICE_PER_M2", fs.pricePerM2)
                intent.putExtra("FLOOR_LABOUR", fs.labourCost)
                startActivity(intent)
            }
        }
    }
}
