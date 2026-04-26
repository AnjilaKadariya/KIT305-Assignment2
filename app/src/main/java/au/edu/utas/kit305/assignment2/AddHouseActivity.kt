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

        houseId = intent.getStringExtra("HOUSE_ID") // null if new project
        isEditMode = houseId != null

        ui.toolbar.title = if (isEditMode) "Edit Project" else "New Project"
        ui.toolbar.setNavigationOnClickListener { finish() }

        if (isEditMode) {
            ui.btnDeleteHouse.visibility = android.view.View.VISIBLE
            ui.toolbar.title = "Edit Project"
            ui.btnSave.text = "Update Project"
        } else {
            ui.btnDeleteHouse.visibility = android.view.View.GONE
        }

        // Delete button with confirmation
        ui.btnDeleteHouse.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete this project? All rooms, windows and floor spaces will be lost!")
                .setPositiveButton("Delete") { _, _ ->
                    val db = Firebase.firestore
                    houseId?.let { id ->
                        db.collection("houses").document(id).delete()
                    }
                    Toast.makeText(this, "Project deleted! 🗑️", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Property type dropdown
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

        if (isEditMode) {
            loadHouseData()
        }

        ui.btnSave.setOnClickListener {
            if (isEditMode) {
                updateHouse()
            } else {
                saveHouse()
            }
        }
    }

    private fun loadHouseData() {
        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id).get()
                .addOnSuccessListener { doc ->
                    ui.txtCustomerName.setText(doc.getString("customerName") ?: "")
                    ui.txtPhone.setText(doc.getString("phone") ?: "")
                    ui.txtEmail.setText(doc.getString("email") ?: "")

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

                    val propertyType = doc.getString("propertyType") ?: ""
                    val propertyTypes = arrayOf("Select Property Type", "House", "Apartment", "Unit", "Townhouse")
                    val index = propertyTypes.indexOf(propertyType)
                    if (index >= 0) ui.spinnerPropertyType.setSelection(index)
                }
        }
    }

    private fun validateFields(): Boolean {
        val customerName = ui.txtCustomerName.text.toString().trim()
        val phone = ui.txtPhone.text.toString().trim().replace(" ", "").replace("-", "")
        val email = ui.txtEmail.text.toString().trim()
        val address = ui.txtAddress.text.toString().trim()
        val suburb = ui.txtSuburb.text.toString().trim()
        val postcode = ui.txtPostcode.text.toString().trim()

        if (customerName.isEmpty()) {
            ui.txtCustomerName.error = "Required"
            ui.txtCustomerName.requestFocus()
            return false
        }
        if (phone.isEmpty()) {
            ui.txtPhone.error = "Required"
            ui.txtPhone.requestFocus()
            return false
        }
        if (phone.length < 8 || phone.length > 12) {
            ui.txtPhone.error = "Invalid phone number"
            ui.txtPhone.requestFocus()
            return false
        }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ui.txtEmail.error = "Invalid email"
            ui.txtEmail.requestFocus()
            return false
        }
        if (address.isEmpty()) {
            ui.txtAddress.error = "Required"
            ui.txtAddress.requestFocus()
            return false
        }
        if (suburb.isEmpty()) {
            ui.txtSuburb.error = "Required"
            ui.txtSuburb.requestFocus()
            return false
        }
        if (postcode.isEmpty()) {
            ui.txtPostcode.error = "Required"
            ui.txtPostcode.requestFocus()
            return false
        }
        if (ui.spinnerPropertyType.selectedItem.toString() == "Select Property Type") {
            Toast.makeText(this, "Please select a property type!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

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
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun updateHouse() {
        if (!validateFields()) return
        if (houseId == null) return

        val phone = ui.txtPhone.text.toString().trim().replace(" ", "").replace("-", "")
        val fullAddress = "${ui.txtAddress.text.toString().trim()}, ${ui.txtSuburb.text.toString().trim()} ${ui.txtPostcode.text.toString().trim()}"

        val db = Firebase.firestore
        db.collection("houses").document(houseId!!)
            .update(
                "customerName", ui.txtCustomerName.text.toString().trim(),
                "address", fullAddress,
                "phone", phone,
                "email", ui.txtEmail.text.toString().trim(),
                "propertyType", ui.spinnerPropertyType.selectedItem.toString(),
                "emergencyContact", ui.txtEmergencyContact.text.toString().trim()
            )

        Toast.makeText(this, "Project updated! ✅", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}