package au.edu.utas.kit305.assignment2

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityQuoteBinding

class QuoteActivity : AppCompatActivity() {

    private lateinit var ui: ActivityQuoteBinding
    private var houseId: String? = null
    private var customerName: String? = null
    private var discountPercent: Double = 0.0
    private var selectedRoomIds: ArrayList<String> = arrayListOf()
    private var grandTotal: Double = 0.0
    private val quoteText = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityQuoteBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.toolbar.setNavigationOnClickListener { finish() }

        houseId = intent.getStringExtra("HOUSE_ID")
        customerName = intent.getStringExtra("CUSTOMER_NAME")
        discountPercent = intent.getDoubleExtra("DISCOUNT", 0.0)
        selectedRoomIds = intent.getStringArrayListExtra("SELECTED_ROOMS") ?: arrayListOf()

        ui.txtCustomerName.text = customerName ?: "Customer"
        ui.txtDiscount.text = "$discountPercent%"

        loadQuote()

        ui.btnShare.setOnClickListener { shareQuote() }
    }

    private fun loadQuote() {
        val db = Firebase.firestore
        var totalBeforeDiscount = 0.0
        var roomsProcessed = 0

        quoteText.append("Quote Summary\n")
        quoteText.append("Customer: $customerName\n\n")

        if (selectedRoomIds.isEmpty()) {
            Toast.makeText(this, "No rooms selected!", Toast.LENGTH_SHORT).show()
            return
        }

        for (roomId in selectedRoomIds) {
            houseId?.let { hid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(roomId).get()
                    .addOnSuccessListener { roomDoc ->
                        val roomName = roomDoc.getString("name") ?: "Room"

                        // Load windows and floor spaces for this room
                        db.collection("houses").document(hid)
                            .collection("rooms").document(roomId)
                            .collection("windows").get()
                            .addOnSuccessListener { windows ->
                                db.collection("houses").document(hid)
                                    .collection("rooms").document(roomId)
                                    .collection("floorspaces").get()
                                    .addOnSuccessListener { floorSpaces ->

                                        var roomTotal = 0.0
                                        val labour = 200.0

                                        // Add room card to UI
                                        val roomView = createRoomView(roomName)
                                        ui.roomsContainer.addView(roomView)

                                        quoteText.append("$roomName\n")

                                        // Add each window
                                        for (doc in windows) {
                                            val window = doc.toObject(Window::class.java)
                                            val price = window.estimatedPrice
                                            val productInfo = if (window.productName != null)
                                                "${window.name} - ${window.productName}"
                                            else window.name ?: "Window"

                                            addItemRow(roomView, productInfo ?: "", price)
                                            quoteText.append("  $productInfo: $${"%.2f".format(price)}\n")
                                            roomTotal += price
                                        }

                                        // Add each floor space
                                        for (doc in floorSpaces) {
                                            val fs = doc.toObject(FloorSpace::class.java)
                                            val price = fs.estimatedPrice
                                            val productInfo = if (fs.productName != null)
                                                "${fs.name} - ${fs.productName}"
                                            else fs.name ?: "Floor Space"

                                            addItemRow(roomView, productInfo ?: "", price)
                                            quoteText.append("  $productInfo: $${"%.2f".format(price)}\n")
                                            roomTotal += price
                                        }

                                        // Add labour
                                        addItemRow(roomView, "Labour", labour, bold = true)
                                        quoteText.append("  Labour: $${"%.2f".format(labour)}\n")
                                        roomTotal += labour

                                        // Add room total
                                        addTotalRow(roomView, "Room Total", roomTotal)
                                        quoteText.append("  Room Total: $${"%.2f".format(roomTotal)}\n\n")

                                        totalBeforeDiscount += roomTotal
                                        roomsProcessed++

                                        // When all rooms processed, show grand total
                                        if (roomsProcessed == selectedRoomIds.size) {
                                            val discountAmount = totalBeforeDiscount * discountPercent / 100
                                            grandTotal = totalBeforeDiscount - discountAmount
                                            ui.txtTotal.text = "$${"%.2f".format(grandTotal)}"
                                            quoteText.append("Discount: $discountPercent%\n")
                                            quoteText.append("Total Quote: $${"%.2f".format(grandTotal)}")
                                        }
                                    }
                            }
                    }
            }
        }
    }

    // Creates a card view for a room
    private fun createRoomView(roomName: String): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(32, 32, 32, 32)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        card.layoutParams = params
        card.setBackgroundColor(android.graphics.Color.parseColor("#1C2128"))

        // Room name
        val title = TextView(this)
        title.text = roomName
        title.textSize = 18f
        title.setTextColor(android.graphics.Color.WHITE)
        title.typeface = android.graphics.Typeface.DEFAULT_BOLD
        title.setPadding(0, 0, 0, 16)
        card.addView(title)

        return card
    }

    // Adds a line item row to a room card
    private fun addItemRow(container: LinearLayout, label: String, price: Double, bold: Boolean = false) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.setPadding(0, 4, 0, 4)

        val labelView = TextView(this)
        labelView.text = label
        labelView.textSize = 14f
        labelView.setTextColor(android.graphics.Color.parseColor("#8B949E"))
        labelView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        if (bold) {
            labelView.typeface = android.graphics.Typeface.DEFAULT_BOLD
            labelView.setTextColor(android.graphics.Color.WHITE)
        }

        val priceView = TextView(this)
        priceView.text = "$${"%.2f".format(price)}"
        priceView.textSize = 14f
        priceView.setTextColor(android.graphics.Color.parseColor("#8B949E"))
        priceView.gravity = Gravity.END
        if (bold) {
            priceView.typeface = android.graphics.Typeface.DEFAULT_BOLD
            priceView.setTextColor(android.graphics.Color.WHITE)
        }

        row.addView(labelView)
        row.addView(priceView)
        container.addView(row)
    }

    // Adds a bold total row to a room card
    private fun addTotalRow(container: LinearLayout, label: String, total: Double) {
        // Divider
        val divider = android.view.View(this)
        divider.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1
        ).also { it.setMargins(0, 16, 0, 16) }
        divider.setBackgroundColor(android.graphics.Color.parseColor("#30363D"))
        container.addView(divider)

        addItemRow(container, label, total, bold = true)
    }

    // Share quote as plain text
    private fun shareQuote() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Floor Me Quote - $customerName")
        shareIntent.putExtra(Intent.EXTRA_TEXT, quoteText.toString())
        startActivity(Intent.createChooser(shareIntent, "Share Quote via"))
    }
}