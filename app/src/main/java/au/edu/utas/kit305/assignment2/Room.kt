package au.edu.utas.kit305.assignment2
import com.google.firebase.firestore.Exclude

class Room (
    @get:Exclude var id: String? = null,
    var name: String? = null
)