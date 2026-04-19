package au.edu.utas.kit305.assignment2

import com.google.firebase.firestore.Exclude

class House (
    @get:Exclude var id: String? = null,
    var customerName: String? = null,
    var address: String? = null,
    var phone: String? = null,
    var propertyType: String? = null
) {
    // Empty constructor required for Firebase
    constructor() : this(null, null, null, null, null)
}