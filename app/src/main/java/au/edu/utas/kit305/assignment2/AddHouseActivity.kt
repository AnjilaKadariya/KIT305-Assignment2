package au.edu.utas.kit305.assignment2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityAddHouseBinding

class AddHouseActivity : AppCompatActivity() {

    private lateinit var ui: ActivityAddHouseBinding
    private var houseId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddHouseBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Check if we are editing an existing house
        houseId = intent.getStringExtra("HOUSE_ID")
        isEditMode = houseId != null

        // Set toolbar title based on mode
        ui.toolbar.title = if (isEditMode) "Edit Project" else "New Project"
        ui.toolbar.setNavigationOnClickListener { finish() }

        // Set up property type dropdown
        val propertyTypes = arrayOf(
            "Select Property Type",
            "House",
            "Apartment",
            "Unit",
            "Townhouse"
        )
        val adapter = ArrayAdapter(this, R.layout.spinner_item, propertyTypes)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        ui.spinnerPropertyType.adapter = adapter

        // If edit mode, load existing data and pre-fill fields
        if (isEditMode) {
            loadHouseData()
        }

        // Save button - calls update or save depending on mode
        ui.btnSave.setOnClickListener {
            if (isEditMode) {
                updateHouse()
            } else {
                saveHouse()
            }
        }
    }

    // Load existing house data and pre-fill all fields
    private fun loadHouseData() {
        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id).get()
                .addOnSuccessListener { doc ->
                    ui.txtCustomerName.setText(doc.getString("customerName") ?: "")
                    ui.txtPhone.setText(doc.getString("phone") ?: "")
                    ui.txtEmail.setText(doc.getString("email") ?: "")

                    // Split address back into parts
                    val fullAddress = doc.getString("address") ?: ""
                    val parts = fullAddress.split(", ")
                    if (parts.size >= 2) {
                        ui.txtAddress.setText(parts[0])
                        val suburbPostcode = parts[1].split(" ")
                        if (suburbPostcode.size >= 2) {
                            ui.txtSuburb.setText(suburbPostcode[0])
                            ui.txtPostcode.setText(suburbPostcode[1])
                        }
                    }

                    ui.txtEmergencyContact.setText(doc.getString("emergencyContact") ?: "")

                    // Set spinner to correct property type
                    val propertyType = doc.getString("propertyType") ?: ""
                    val propertyTypes = arrayOf("Select Property Type", "House", "Apartment", "Unit", "Townhouse")
                    val index = propertyTypes.indexOf(propertyType)
                    if (index >= 0) ui.spinnerPropertyType.setSelection(index)
                }
        }
    }

    // Validate all fields and return true if valid
    private fun validateFields(): Boolean {
        val customerName = ui.txtCustomerName.text.toString().trim()
        val phone = ui.txtPhone.text.toString().trim().replace(" ", "").replace("-", "")
        val email = ui.txtEmail.text.toString().trim()
        val address = ui.txtAddress.text.toString().trim()
        val suburb = ui.txtSuburb.text.toString().trim()
        val postcode = ui.txtPostcode.text.toString().trim()

        if (customerName.isEmpty()) {
            ui.txtCustomerName.error = "Customer name is required"
            ui.txtCustomerName.requestFocus()
            return false
        }
        if (phone.isEmpty()) {
            ui.txtPhone.error = "Phone number is required"
            ui.txtPhone.requestFocus()
            return false
        }
        if (phone.length != 10) {
            ui.txtPhone.error = "Phone number must be exactly 10 digits"
            ui.txtPhone.requestFocus()
            return false
        }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ui.txtEmail.error = "Please enter a valid email address"
            ui.txtEmail.requestFocus()
            return false
        }
        if (address.isEmpty()) {
            ui.txtAddress.error = "Address is required"
            ui.txtAddress.requestFocus()
            return false
        }
        if (suburb.isEmpty()) {
            ui.txtSuburb.error = "Suburb is required"
            ui.txtSuburb.requestFocus()
            return false
        }
        if (postcode.isEmpty()) {
            ui.txtPostcode.error = "Postcode is required"
            ui.txtPostcode.requestFocus()
            return false
        }
        if (ui.spinnerPropertyType.selectedItem.toString() == "Select Property Type") {
            Toast.makeText(this, "Please select a property type!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // Save new house to Firebase
    private fun saveHouse() {
        if (!validateFields()) return

        val phone = ui.txtPhone.text.toString().trim().replace(" ", "").replace("-", "")
        val fullAddress = "${ui.txtAddress.text.toString().trim()}, ${ui.txtSuburb.text.toString().trim()} ${ui.txtPostcode.text.toString().trim()}"

        val house = House(
            customerName = ui.txtCustomerName.text.toString().trim(),
            address = fullAddress,
            phone = phone,
            propertyType = ui.spinnerPropertyType.selectedItem.toString()
        )

        val db = Firebase.firestore
        db.collection("houses").add(house)
        Toast.makeText(this, "Project saved! 🎉", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // Update existing house in Firebase
    private fun updateHouse() {
        if (!validateFields()) return

        val phone = ui.txtPhone.text.toString().trim().replace(" ", "").replace("-", "")
        val fullAddress = "${ui.txtAddress.text.toString().trim()}, ${ui.txtSuburb.text.toString().trim()} ${ui.txtPostcode.text.toString().trim()}"

        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id)
                .update(
                    "customerName", ui.txtCustomerName.text.toString().trim(),
                    "address", fullAddress,
                    "phone", phone,
                    "propertyType", ui.spinnerPropertyType.selectedItem.toString()
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Project updated! ✅", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating project", Toast.LENGTH_SHORT).show()
                }
        }
    }
}