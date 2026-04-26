package au.edu.utas.kit305.assignment2

import com.google.firebase.firestore.Exclude

class Window (
    @get:Exclude var id: String? = null, // not stored in firestore
    var name: String? = null,
    var width: Float? = null,
    var height: Float? = null,
    var productName: String? = null,
    var productColour: String? = null,
    var pricePerM2: Double = 0.0,
    var labourCost: Double = 0.0,
    var estimatedPrice: Double = 0.0 // calculated before saving
)