package au.edu.utas.kit305.assignment2

import android.app.Activity
import android.content.Intent
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

    private var selectedProductName: String? = null
    private var selectedProductPricePerM2: Double = 0.0
    private var selectedProductLabour: Double = 0.0
    private var selectedProductColour: String? = null

    companion object {
        const val REQUEST_PRODUCT = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddFloorSpaceBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")
        floorId = intent.getStringExtra("FLOOR_ID")
        isEditMode = floorId != null

        ui.toolbar.title = if (isEditMode) "Edit Floor Space" else "Add Floor Space"
        ui.toolbar.setNavigationOnClickListener { finish() }

        if (isEditMode) {
            ui.txtFloorSpaceName.setText(intent.getStringExtra("FLOOR_NAME"))
            ui.txtWidth.setText(intent.getFloatExtra("FLOOR_WIDTH", 0f).toString())
            ui.txtDepth.setText(intent.getFloatExtra("FLOOR_DEPTH", 0f).toString())

            val savedProduct = intent.getStringExtra("FLOOR_PRODUCT")
            val savedColour = intent.getStringExtra("FLOOR_COLOUR")
            val savedPrice = intent.getDoubleExtra("FLOOR_PRICE", 0.0)

            if (savedProduct != null) {
                selectedProductName = savedProduct
                selectedProductColour = savedColour
                selectedProductPricePerM2 = intent.getDoubleExtra("FLOOR_PRICE_PER_M2", 0.0)
                selectedProductLabour = intent.getDoubleExtra("FLOOR_LABOUR", 0.0)
                ui.txtProductName.text = "$savedProduct ($savedColour)"
                ui.txtProductName.setTextColor(android.graphics.Color.parseColor("#1A1A1A"))
                ui.txtEstimatedPrice.text = "$${"%.2f".format(savedPrice)}"
                ui.txtEstimatedPrice.setTextColor(android.graphics.Color.parseColor("#1B8A5A"))
            }
        }

        ui.btnCancel.setOnClickListener { finish() }

        ui.btnChooseProduct.setOnClickListener {
            val widthStr = ui.txtWidth.text.toString().trim()
            val depthStr = ui.txtDepth.text.toString().trim()
            if (widthStr.isEmpty() || depthStr.isEmpty()) {
                Toast.makeText(this, "Please enter width and depth first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SelectProductActivity::class.java)
            intent.putExtra("FOR_TYPE", "floorspace")
            intent.putExtra("FLOOR_WIDTH", widthStr.toFloatOrNull() ?: 0f)
            intent.putExtra("FLOOR_DEPTH", depthStr.toFloatOrNull() ?: 0f)
            startActivityForResult(intent, REQUEST_PRODUCT)
        }

        ui.btnSave.setOnClickListener {
            if (isEditMode) updateFloorSpace() else saveFloorSpace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PRODUCT && resultCode == Activity.RESULT_OK && data != null) {
            selectedProductName = data.getStringExtra("PRODUCT_NAME")
            selectedProductPricePerM2 = data.getDoubleExtra("PRODUCT_PRICE_PER_M2", 0.0)
            selectedProductLabour = data.getDoubleExtra("PRODUCT_LABOUR", 0.0)
            selectedProductColour = data.getStringExtra("PRODUCT_COLOUR")

            ui.txtProductName.text = "$selectedProductName ($selectedProductColour)"
            ui.txtProductName.setTextColor(android.graphics.Color.parseColor("#1A1A1A"))

            updateEstimatedPrice()
        }
    }

    private fun updateEstimatedPrice() {
        val width = ui.txtWidth.text.toString().toFloatOrNull() ?: return
        val depth = ui.txtDepth.text.toString().toFloatOrNull() ?: return
        val area = width * depth
        val price = area * selectedProductPricePerM2 + selectedProductLabour
        ui.txtEstimatedPrice.text = "$${"%.2f".format(price)}"
        ui.txtEstimatedPrice.setTextColor(android.graphics.Color.parseColor("#1B8A5A"))
    }

    private fun validateFields(): Triple<String, Float, Float>? {
        val name = ui.txtFloorSpaceName.text.toString().trim()
        val widthStr = ui.txtWidth.text.toString().trim()
        val depthStr = ui.txtDepth.text.toString().trim()

        if (name.isEmpty()) { ui.txtFloorSpaceName.error = "Required"; ui.txtFloorSpaceName.requestFocus(); return null }
        if (widthStr.isEmpty()) { ui.txtWidth.error = "Required"; ui.txtWidth.requestFocus(); return null }
        if (depthStr.isEmpty()) { ui.txtDepth.error = "Required"; ui.txtDepth.requestFocus(); return null }

        val width = widthStr.toFloatOrNull()
        val depth = depthStr.toFloatOrNull()

        if (width == null || width <= 0) { ui.txtWidth.error = "Invalid width"; ui.txtWidth.requestFocus(); return null }
        if (depth == null || depth <= 0) { ui.txtDepth.error = "Invalid depth"; ui.txtDepth.requestFocus(); return null }

        return Triple(name, width, depth)
    }

    private fun saveFloorSpace() {
        val fields = validateFields() ?: return
        val (name, width, depth) = fields

        val area = width * depth
        val estimatedPrice = if (selectedProductPricePerM2 > 0) area * selectedProductPricePerM2 + selectedProductLabour else 0.0

        val floorSpace = FloorSpace(
            name = name,
            width = width,
            depth = depth,
            productName = selectedProductName,
            productColour = selectedProductColour,
            pricePerM2 = selectedProductPricePerM2,
            labourCost = selectedProductLabour,
            estimatedPrice = estimatedPrice
        )

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("floorspaces").add(floorSpace)
                Toast.makeText(this, "Floor space added! 🏠", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateFloorSpace() {
        val fields = validateFields() ?: return
        val (name, width, depth) = fields

        val area = width * depth
        val estimatedPrice = if (selectedProductPricePerM2 > 0) area * selectedProductPricePerM2 + selectedProductLabour else 0.0

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                floorId?.let { fid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("floorspaces").document(fid)
                        .update(
                            "name", name,
                            "width", width,
                            "depth", depth,
                            "productName", selectedProductName,
                            "productColour", selectedProductColour,
                            "pricePerM2", selectedProductPricePerM2,
                            "labourCost", selectedProductLabour,
                            "estimatedPrice", estimatedPrice
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Floor space updated! ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
            }
        }
    }
}