package au.edu.utas.kit305.assignment2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityAddFloorSpaceBinding

class AddFloorSpaceActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddFloorSpaceBinding
    private var houseId: String? = null
    private var roomId: String? = null
    private var floorId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddFloorSpaceBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Get IDs passed from RoomDetailActivity
        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")
        floorId = intent.getStringExtra("FLOOR_ID")

        // If floorId exists, we are in edit mode
        isEditMode = floorId != null

        // Set toolbar title based on mode
        ui.toolbar.title = if (isEditMode) "Edit Floor Space" else "Add Floor Space"
        ui.toolbar.setNavigationOnClickListener { finish() }

        // If edit mode, pre-fill fields with existing data
        if (isEditMode) {
            ui.txtFloorSpaceName.setText(intent.getStringExtra("FLOOR_NAME"))
            ui.txtWidth.setText(intent.getFloatExtra("FLOOR_WIDTH", 0f).toString())
            ui.txtDepth.setText(intent.getFloatExtra("FLOOR_DEPTH", 0f).toString())
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
                updateFloorSpace()
            } else {
                saveFloorSpace()
            }
        }
    }

    // Validate fields and return values if all good
    private fun validateFields(): Triple<String, Float, Float>? {
        val name = ui.txtFloorSpaceName.text.toString().trim()
        val widthStr = ui.txtWidth.text.toString().trim()
        val depthStr = ui.txtDepth.text.toString().trim()

        if (name.isEmpty()) {
            ui.txtFloorSpaceName.error = "Floor space name is required"
            ui.txtFloorSpaceName.requestFocus()
            return null
        }
        if (widthStr.isEmpty()) {
            ui.txtWidth.error = "Width is required"
            ui.txtWidth.requestFocus()
            return null
        }
        if (depthStr.isEmpty()) {
            ui.txtDepth.error = "Depth is required"
            ui.txtDepth.requestFocus()
            return null
        }

        val width = widthStr.toFloatOrNull()
        val depth = depthStr.toFloatOrNull()

        if (width == null || width <= 0) {
            ui.txtWidth.error = "Please enter a valid width in m"
            ui.txtWidth.requestFocus()
            return null
        }
        if (depth == null || depth <= 0) {
            ui.txtDepth.error = "Please enter a valid depth in m"
            ui.txtDepth.requestFocus()
            return null
        }

        return Triple(name, width, depth)
    }

    // Save new floor space to Firebase
    private fun saveFloorSpace() {
        val fields = validateFields() ?: return
        val (name, width, depth) = fields

        val floorSpace = FloorSpace(name = name, width = width, depth = depth)

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("floorspaces")
                    .add(floorSpace)
                Toast.makeText(this, "Floor space added! 🏠", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // Update existing floor space in Firebase
    private fun updateFloorSpace() {
        val fields = validateFields() ?: return
        val (name, width, depth) = fields

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                floorId?.let { fid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("floorspaces").document(fid)
                        .update("name", name, "width", width, "depth", depth)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Floor space updated! ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error updating floor space", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}