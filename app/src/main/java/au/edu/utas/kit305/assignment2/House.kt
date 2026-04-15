package au.edu.utas.kit305.assignment2

import com.google.firebase.firestore.Exclude

class House {
    @get:Exclude var id: String? = null,  // Firebase document ID
    var customerName: String? = null,      // Customer's name
    var address: String? = null            // Property address


}