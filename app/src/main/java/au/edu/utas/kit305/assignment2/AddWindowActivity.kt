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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddWindowBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")

        ui.toolbar.setNavigationOnClickListener { finish() }

        // Cancel button
        ui.btnCancel.setOnClickListener { finish() }

        // Choose product - coming soon
        ui.btnChooseProduct.setOnClickListener {
            Toast.makeText(this, "Product selection coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Save button
        ui.btnSave.setOnClickListener {
            saveWindow()
        }
    }

    private fun saveWindow() {
        val name = ui.txtWindowName.text.toString().trim()
        val widthStr = ui.txtWidth.text.toString().trim()
        val heightStr = ui.txtHeight.text.toString().trim()

        if (name.isEmpty()) {
            ui.txtWindowName.error = "Window name is required"
            ui.txtWindowName.requestFocus()
            return
        }
        if (widthStr.isEmpty()) {
            ui.txtWidth.error = "Width is required"
            ui.txtWidth.requestFocus()
            return
        }
        if (heightStr.isEmpty()) {
            ui.txtHeight.error = "Height is required"
            ui.txtHeight.requestFocus()
            return
        }

        val width = widthStr.toDoubleOrNull()
        val height = heightStr.toDoubleOrNull()

        if (width == null || width <= 0) {
            ui.txtWidth.error = "Please enter a valid width in mm"
            ui.txtWidth.requestFocus()
            return
        }
        if (height == null || height <= 0) {
            ui.txtHeight.error = "Please enter a valid height in mm"
            ui.txtHeight.requestFocus()
            return
        }

        val window = Window(name = name, width = width.toFloat(), height = height.toFloat())

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
}