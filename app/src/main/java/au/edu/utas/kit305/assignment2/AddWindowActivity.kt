package au.edu.utas.kit305.assignment2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityAddWindowBinding

class AddWindowActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddWindowBinding
    private var houseId: String? = null
    private var roomId: String? = null
    private var windowId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddWindowBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Get IDs passed from RoomDetailActivity
        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")
        windowId = intent.getStringExtra("WINDOW_ID")

        // If windowId exists, we are in edit mode
        isEditMode = windowId != null

        // Set toolbar title based on mode
        ui.toolbar.title = if (isEditMode) "Edit Window" else "Add Window"
        ui.toolbar.setNavigationOnClickListener { finish() }

        // If edit mode, pre-fill fields with existing data
        if (isEditMode) {
            ui.txtWindowName.setText(intent.getStringExtra("WINDOW_NAME"))
            ui.txtWidth.setText(intent.getFloatExtra("WINDOW_WIDTH", 0f).toString())
            ui.txtHeight.setText(intent.getFloatExtra("WINDOW_HEIGHT", 0f).toString())
        }

        // Cancel button closes the screen
        ui.btnCancel.setOnClickListener { finish() }

        // Choose product button - will open SelectProductActivity later
        ui.btnChooseProduct.setOnClickListener {
            Toast.makeText(this, "Product selection coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Save button - calls update or save depending on mode
        ui.btnSave.setOnClickListener {
            if (isEditMode) {
                updateWindow()
            } else {
                saveWindow()
            }
        }
    }

    // Validate fields and return true if all good
    private fun validateFields(): Triple<String, Float, Float>? {
        val name = ui.txtWindowName.text.toString().trim()
        val widthStr = ui.txtWidth.text.toString().trim()
        val heightStr = ui.txtHeight.text.toString().trim()

        if (name.isEmpty()) {
            ui.txtWindowName.error = "Window name is required"
            ui.txtWindowName.requestFocus()
            return null
        }
        if (widthStr.isEmpty()) {
            ui.txtWidth.error = "Width is required"
            ui.txtWidth.requestFocus()
            return null
        }
        if (heightStr.isEmpty()) {
            ui.txtHeight.error = "Height is required"
            ui.txtHeight.requestFocus()
            return null
        }

        val width = widthStr.toFloatOrNull()
        val height = heightStr.toFloatOrNull()

        if (width == null || width <= 0) {
            ui.txtWidth.error = "Please enter a valid width in mm"
            ui.txtWidth.requestFocus()
            return null
        }
        if (height == null || height <= 0) {
            ui.txtHeight.error = "Please enter a valid height in mm"
            ui.txtHeight.requestFocus()
            return null
        }

        return Triple(name, width, height)
    }

    // Save new window to Firebase
    private fun saveWindow() {
        val fields = validateFields() ?: return
        val (name, width, height) = fields

        val window = Window(name = name, width = width, height = height)

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("windows")
                    .add(window)
                Toast.makeText(this, "Window added! 🪟", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Update existing window in Firebase
    private fun updateWindow() {
        val fields = validateFields() ?: return
        val (name, width, height) = fields

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                windowId?.let { wid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("windows").document(wid)
                        .update("name", name, "width", width, "height", height)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Window updated! ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error updating window", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}