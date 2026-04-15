package au.edu.utas.kit305.assignment2

import com.google.firebase.firestore.Exclude

class Window (
    @get:Exclude var id: String? = null,
    var name: String? = null,
    var width: Float? = null,
    var height: Float? = null
)