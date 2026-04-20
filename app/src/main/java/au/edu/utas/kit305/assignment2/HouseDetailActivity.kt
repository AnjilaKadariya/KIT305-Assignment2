package au.edu.utas.kit305.assignment2

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityHouseDetailBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemRoomBinding

class HouseDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityHouseDetailBinding
    private var houseId: String? = null
    private val rooms = mutableListOf<Room>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityHouseDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        val houseName = intent.getStringExtra("HOUSE_NAME")

        // Toolbar
        ui.toolbar.title = houseName ?: "Project Details"
        ui.toolbar.setNavigationOnClickListener { finish() }

        // RecyclerView
        ui.roomList.layoutManager = LinearLayoutManager(this)
        ui.roomList.adapter = RoomAdapter(rooms)

        // Load house details
        loadHouseDetails()

        // Load rooms
        loadRooms()

        // Add room button
        ui.btnAddRoom.setOnClickListener {
            showAddRoomDialog()
        }
    }

    private fun loadHouseDetails() {
        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id).get()
                .addOnSuccessListener { doc ->
                    ui.txtPropertyType.text = doc.getString("propertyType") ?: ""
                    ui.txtAddress.text = doc.getString("address") ?: ""
                }
        }
    }

    private fun loadRooms() {
        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id).collection("rooms")
                .get()
                .addOnSuccessListener { result ->
                    rooms.clear()
                    for (doc in result) {
                        val room = doc.toObject(Room::class.java)
                        room.id = doc.id
                        rooms.add(room)
                    }
                    if (rooms.isEmpty()) {
                        ui.txtEmpty.visibility = View.VISIBLE
                        ui.roomList.visibility = View.GONE
                    } else {
                        ui.txtEmpty.visibility = View.GONE
                        ui.roomList.visibility = View.VISIBLE
                    }
                    (ui.roomList.adapter as RoomAdapter).notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Log.e(FIREBASE_TAG, "Error loading rooms", it)
                }
        }
    }

    private fun showAddRoomDialog() {
        val input = EditText(this)
        input.hint = "e.g. Bedroom"
        input.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this)
            .setTitle("Add Room")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter a room name!", Toast.LENGTH_SHORT).show()
                } else {
                    saveRoom(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveRoom(name: String) {
        val db = Firebase.firestore
        val room = Room(name = name)
        houseId?.let { id ->
            db.collection("houses").document(id).collection("rooms")
                .add(room)
                .addOnSuccessListener {
                    Toast.makeText(this, "Room added! 🏠", Toast.LENGTH_SHORT).show()
                    loadRooms()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error adding room", Toast.LENGTH_SHORT).show()
                }
        }
    }

    inner class RoomHolder(var ui: ListItemRoomBinding) : RecyclerView.ViewHolder(ui.root)

    inner class RoomAdapter(private val roomList: MutableList<Room>) :
        RecyclerView.Adapter<RoomHolder>() {

        override fun getItemCount() = roomList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
            val ui = ListItemRoomBinding.inflate(layoutInflater, parent, false)
            return RoomHolder(ui)
        }

        override fun onBindViewHolder(holder: RoomHolder, position: Int) {
            val room = roomList[position]
            holder.ui.txtRoomName.text = room.name ?: "No name"

            holder.itemView.setOnClickListener {
                val intent = android.content.Intent(this@HouseDetailActivity, RoomDetailActivity::class.java)
                intent.putExtra("HOUSE_ID", houseId)
                intent.putExtra("ROOM_ID", room.id)
                intent.putExtra("ROOM_NAME", room.name)
                startActivity(intent)
            }
        }
    }
}