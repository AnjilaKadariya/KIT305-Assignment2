package au.edu.utas.kit305.assignment2

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

class House (
    @get:Exclude var id: String? = null, // not stored in firestore
    var customerName: String? = null,
    var address: String? = null,
    var phone: String? = null,
    var propertyType: String? = null,
    @ServerTimestamp var createdAt: Date? = null // auto-set by firebase
) {
    constructor() : this(null, null, null, null, null, null)
}