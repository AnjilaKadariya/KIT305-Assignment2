package au.edu.utas.kit305.assignment2

import android.app.Activity
import android.content.Intent
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

    // Store selected product info
    private var selectedProductName: String? = null
    private var selectedProductPricePerM2: Double = 0.0
    private var selectedProductLabour: Double = 0.0
    private var selectedProductColour: String? = null

    companion object {
        const val REQUEST_PRODUCT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddWindowBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")
        windowId = intent.getStringExtra("WINDOW_ID")
        isEditMode = windowId != null

        ui.toolbar.title = if (isEditMode) "Edit Window" else "Add Window"
        ui.toolbar.setNavigationOnClickListener { finish() }

        if (isEditMode) {
            ui.txtWindowName.setText(intent.getStringExtra("WINDOW_NAME"))
            ui.txtWidth.setText(intent.getFloatExtra("WINDOW_WIDTH", 0f).toString())
            ui.txtHeight.setText(intent.getFloatExtra("WINDOW_HEIGHT", 0f).toString())

            // Also load saved product info
            val savedProduct = intent.getStringExtra("WINDOW_PRODUCT")
            val savedColour = intent.getStringExtra("WINDOW_COLOUR")
            val savedPrice = intent.getDoubleExtra("WINDOW_PRICE", 0.0)

            if (savedProduct != null) {
                selectedProductName = savedProduct
                selectedProductColour = savedColour
                selectedProductPricePerM2 = intent.getDoubleExtra("WINDOW_PRICE_PER_M2", 0.0)
                selectedProductLabour = intent.getDoubleExtra("WINDOW_LABOUR", 0.0)
                ui.txtProductName.text = "$savedProduct ($savedColour)"
                ui.txtProductName.setTextColor(android.graphics.Color.WHITE)
                ui.txtEstimatedPrice.text = "$${"%.2f".format(savedPrice)}"
                ui.txtEstimatedPrice.setTextColor(android.graphics.Color.parseColor("#00BFA5"))
            }
        }

        ui.btnCancel.setOnClickListener { finish() }

        // Choose product → open SelectProductActivity
        ui.btnChooseProduct.setOnClickListener {
            val widthStr = ui.txtWidth.text.toString().trim()
            val heightStr = ui.txtHeight.text.toString().trim()
            if (widthStr.isEmpty() || heightStr.isEmpty()) {
                Toast.makeText(this, "Please enter width and height first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SelectProductActivity::class.java)
            intent.putExtra("FOR_TYPE", "window")
            intent.putExtra("WINDOW_WIDTH", widthStr.toFloatOrNull() ?: 0f)
            intent.putExtra("WINDOW_HEIGHT", heightStr.toFloatOrNull() ?: 0f)
            startActivityForResult(intent, REQUEST_PRODUCT)
        }

        ui.btnSave.setOnClickListener {
            if (isEditMode) updateWindow() else saveWindow()
        }
    }

    // Receive selected product back from SelectProductActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PRODUCT && resultCode == Activity.RESULT_OK && data != null) {
            selectedProductName = data.getStringExtra("PRODUCT_NAME")
            selectedProductPricePerM2 = data.getDoubleExtra("PRODUCT_PRICE_PER_M2", 0.0)
            selectedProductLabour = data.getDoubleExtra("PRODUCT_LABOUR", 0.0)
            selectedProductColour = data.getStringExtra("PRODUCT_COLOUR")

            // Show selected product name
            ui.txtProductName.text = "$selectedProductName (${selectedProductColour})"
            ui.txtProductName.setTextColor(android.graphics.Color.WHITE)

            // Calculate and show estimated price
            updateEstimatedPrice()
        }
    }

    private fun updateEstimatedPrice() {
        val widthMm = ui.txtWidth.text.toString().toFloatOrNull() ?: return
        val heightMm = ui.txtHeight.text.toString().toFloatOrNull() ?: return
        val widthM = widthMm / 1000.0
        val heightM = heightMm / 1000.0
        val area = widthM * heightM
        val price = area * selectedProductPricePerM2 + selectedProductLabour
        ui.txtEstimatedPrice.text = "$${"%.2f".format(price)}"
        ui.txtEstimatedPrice.setTextColor(android.graphics.Color.parseColor("#00BFA5"))
    }

    private fun validateFields(): Triple<String, Float, Float>? {
        val name = ui.txtWindowName.text.toString().trim()
        val widthStr = ui.txtWidth.text.toString().trim()
        val heightStr = ui.txtHeight.text.toString().trim()

        if (name.isEmpty()) { ui.txtWindowName.error = "Required"; ui.txtWindowName.requestFocus(); return null }
        if (widthStr.isEmpty()) { ui.txtWidth.error = "Required"; ui.txtWidth.requestFocus(); return null }
        if (heightStr.isEmpty()) { ui.txtHeight.error = "Required"; ui.txtHeight.requestFocus(); return null }

        val width = widthStr.toFloatOrNull()
        val height = heightStr.toFloatOrNull()

        if (width == null || width <= 0) { ui.txtWidth.error = "Invalid width"; ui.txtWidth.requestFocus(); return null }
        if (height == null || height <= 0) { ui.txtHeight.error = "Invalid height"; ui.txtHeight.requestFocus(); return null }

        return Triple(name, width, height)
    }

    private fun saveWindow() {
        val fields = validateFields() ?: return
        val (name, width, height) = fields

        val widthM = width / 1000.0
        val heightM = height / 1000.0
        val area = widthM * heightM
        val estimatedPrice = if (selectedProductPricePerM2 > 0) area * selectedProductPricePerM2 + selectedProductLabour else 0.0

        val window = Window(
            name = name,
            width = width,
            height = height,
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
                    .collection("windows").add(window)
                Toast.makeText(this, "Window added! 🪟", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateWindow() {
        val fields = validateFields() ?: return
        val (name, width, height) = fields

        val widthM = width / 1000.0
        val heightM = height / 1000.0
        val area = widthM * heightM
        val estimatedPrice = if (selectedProductPricePerM2 > 0) area * selectedProductPricePerM2 + selectedProductLabour else 0.0

        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                windowId?.let { wid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("windows").document(wid)
                        .update(
                            "name", name,
                            "width", width,
                            "height", height,
                            "productName", selectedProductName,
                            "productColour", selectedProductColour,
                            "pricePerM2", selectedProductPricePerM2,
                            "labourCost", selectedProductLabour,
                            "estimatedPrice", estimatedPrice
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Window updated! ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                }
            }
        }
    }
}