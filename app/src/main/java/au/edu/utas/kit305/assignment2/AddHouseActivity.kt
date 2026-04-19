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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAddHouseBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Set up toolbar with back button
        ui.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Set up property type dropdown
        val propertyTypes = arrayOf(
            "Select Property Type",
            "House",
            "Apartment",
            "Unit",
            "Townhouse"
        )
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            propertyTypes
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        ui.spinnerPropertyType.adapter = adapter

        // Save button click
        ui.btnSave.setOnClickListener {
            saveHouse()
        }
    }

    private fun saveHouse() {
        // Get values from fields
        android.util.Log.d(FIREBASE_TAG, "Save clicked!")
        val customerName = ui.txtCustomerName.text.toString().trim()
        val phone = ui.txtPhone.text.toString().trim().replace(" ", "").replace("-", "")
        val email = ui.txtEmail.text.toString().trim()
        val address = ui.txtAddress.text.toString().trim()
        val suburb = ui.txtSuburb.text.toString().trim()
        val postcode = ui.txtPostcode.text.toString().trim()
        val propertyType = ui.spinnerPropertyType.selectedItem.toString()
        val emergencyContact = ui.txtEmergencyContact.text.toString().trim()

        // Validate required fields
        if (customerName.isEmpty()) {
            ui.txtCustomerName.error = "Customer name is required"
            ui.txtCustomerName.requestFocus()
            return
        }
        if (phone.isEmpty()) {
            ui.txtPhone.error = "Phone number is required"
            ui.txtPhone.requestFocus()
            return
        }
        if (phone.length != 10) {
            ui.txtPhone.error = "Phone number must be exactly 10 digits"
            ui.txtPhone.requestFocus()
            return
        }
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            ui.txtEmail.error = "Please enter a valid email address"
            ui.txtEmail.requestFocus()
            return
        }
        if (address.isEmpty()) {
            ui.txtAddress.error = "Address is required"
            ui.txtAddress.requestFocus()
            return
        }
        if (suburb.isEmpty()) {
            ui.txtSuburb.error = "Suburb is required"
            ui.txtSuburb.requestFocus()
            return
        }
        if (postcode.isEmpty()) {
            ui.txtPostcode.error = "Postcode is required"
            ui.txtPostcode.requestFocus()
            return
        }
        if (ui.spinnerPropertyType.selectedItem.toString() == "Select Property Type") {
            Toast.makeText(this, "Please select a property type!", Toast.LENGTH_SHORT).show()
            return
        }

        // Create full address string
        val fullAddress = "$address, $suburb $postcode"

        // Create House object
        val house = House(
            customerName = customerName,
            address = fullAddress,
            phone = phone,
            propertyType = propertyType
        )

// Save to Firebase
        val db = Firebase.firestore
        db.collection("houses").add(house)
        Toast.makeText(this@AddHouseActivity, "Project saved! 🎉", Toast.LENGTH_SHORT).show()
        val intent = Intent(this@AddHouseActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}