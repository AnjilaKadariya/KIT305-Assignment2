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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddFloorSpaceBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")

        ui.toolbar.setNavigationOnClickListener { finish() }
        ui.btnCancel.setOnClickListener { finish() }

        ui.btnChooseProduct.setOnClickListener {
            Toast.makeText(this, "Product selection coming soon!", Toast.LENGTH_SHORT).show()
        }

        ui.btnSave.setOnClickListener {
            saveFloorSpace()
        }
    }

    private fun saveFloorSpace() {
        val name = ui.txtFloorSpaceName.text.toString().trim()
        val widthStr = ui.txtWidth.text.toString().trim()
        val depthStr = ui.txtDepth.text.toString().trim()

        if (name.isEmpty()) {
            ui.txtFloorSpaceName.error = "Floor space name is required"
            ui.txtFloorSpaceName.requestFocus()
            return
        }
        if (widthStr.isEmpty()) {
            ui.txtWidth.error = "Width is required"
            ui.txtWidth.requestFocus()
            return
        }
        if (depthStr.isEmpty()) {
            ui.txtDepth.error = "Depth is required"
            ui.txtDepth.requestFocus()
            return
        }

        val width = widthStr.toDoubleOrNull()
        val depth = depthStr.toDoubleOrNull()

        if (width == null || width <= 0) {
            ui.txtWidth.error = "Please enter a valid width in m"
            ui.txtWidth.requestFocus()
            return
        }
        if (depth == null || depth <= 0) {
            ui.txtDepth.error = "Please enter a valid depth in m"
            ui.txtDepth.requestFocus()
            return
        }

        val floorSpace = FloorSpace(name = name, width = width.toFloat(), depth = depth.toFloat())

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
}