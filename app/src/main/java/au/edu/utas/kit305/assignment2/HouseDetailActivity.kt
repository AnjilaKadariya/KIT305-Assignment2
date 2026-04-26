package au.edu.utas.kit305.assignment2

import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityHouseDetailBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemRoomBinding

class HouseDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityHouseDetailBinding
    private var houseId: String? = null
    private val rooms = mutableListOf<Room>() // loaded from firestore
    private val selectedRooms = mutableSetOf<String>() // for quote generation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityHouseDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        val houseName = intent.getStringExtra("HOUSE_NAME")

        ui.toolbar.title = houseName ?: "Project Details"
        ui.toolbar.setNavigationOnClickListener { finish() }

        ui.btnEditHouse.setOnClickListener {
            val intent = android.content.Intent(this, AddHouseActivity::class.java)
            intent.putExtra("HOUSE_ID", houseId)
            startActivity(intent)
        }

        ui.roomList.layoutManager = LinearLayoutManager(this)
        ui.roomList.adapter = RoomAdapter(rooms)

        // Swipe to delete
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()
                paint.color = Color.parseColor("#CC0000")
                c.drawRect(
                    itemView.right + dX, itemView.top.toFloat(),
                    itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                )
                val icon = ContextCompat.getDrawable(this@HouseDetailActivity, android.R.drawable.ic_menu_delete)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val room = rooms[position]
                AlertDialog.Builder(this@HouseDetailActivity)
                    .setTitle("Delete Room")
                    .setMessage("Are you sure you want to delete '${room.name}'?")
                    .setPositiveButton("Delete") { _, _ -> deleteRoom(room, position) }
                    .setNegativeButton("Cancel") { _, _ ->
                        ui.roomList.adapter?.notifyDataSetChanged()
                    }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(ui.roomList)

        loadHouseDetails()
        loadRooms()

        ui.btnAddRoom.setOnClickListener { showAddRoomDialog() }

        var discountPercent = 0.0
        ui.btnDiscount.setOnClickListener {
            val input = EditText(this)
            input.hint = "e.g. 10"
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.setPadding(48, 32, 48, 32)
            if (discountPercent > 0) input.setText(discountPercent.toString())

            AlertDialog.Builder(this)
                .setTitle("Add Discount %")
                .setMessage("Enter discount percentage (e.g. 20 for 20%)")
                .setView(input)
                .setPositiveButton("Apply") { _, _ ->
                    val value = input.text.toString().trim().toDoubleOrNull()
                    if (value == null || value < 0 || value > 100) {
                        Toast.makeText(this, "Please enter a valid discount (0-100)", Toast.LENGTH_SHORT).show()
                    } else {
                        discountPercent = value
                        ui.btnDiscount.text = if (value > 0) "Discount: $value%" else "+ Discount"
                        Toast.makeText(this, "Discount set to $value%!", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        ui.btnGenerateQuote.setOnClickListener {
            if (selectedRooms.isEmpty()) {
                Toast.makeText(this, "Please select at least one room!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val db = Firebase.firestore
            houseId?.let { id ->
                db.collection("houses").document(id).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("customerName") ?: "Customer"
                        val intent = android.content.Intent(this, QuoteActivity::class.java)
                        intent.putExtra("HOUSE_ID", houseId)
                        intent.putExtra("CUSTOMER_NAME", name)
                        intent.putExtra("DISCOUNT", discountPercent)
                        intent.putStringArrayListExtra("SELECTED_ROOMS", ArrayList(selectedRooms))
                        startActivity(intent)
                    }
            }
        }
    }

    private fun deleteRoom(room: Room, position: Int) {
        val db = Firebase.firestore
        houseId?.let { id ->
            room.id?.let { rid ->
                db.collection("houses").document(id)
                    .collection("rooms").document(rid)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Room deleted!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error deleting room", Toast.LENGTH_SHORT).show()
                        ui.roomList.adapter?.notifyDataSetChanged()
                    }
            }
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

    override fun onResume() {
        super.onResume()
        loadHouseDetails()
        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id).get()
                .addOnSuccessListener { doc ->
                    ui.toolbar.title = doc.getString("customerName") ?: "Project Details"
                }
        }
    }

    private fun loadRooms() {
        val db = Firebase.firestore
        houseId?.let { id ->
            db.collection("houses").document(id).collection("rooms")
                .addSnapshotListener { result, error ->
                    if (error != null) return@addSnapshotListener
                    rooms.clear()
                    for (doc in result!!) {
                        val room = doc.toObject(Room::class.java)
                        room.id = doc.id // store doc id
                        rooms.add(room)
                    }
                    if (rooms.isEmpty()) {
                        ui.txtEmpty.visibility = View.VISIBLE
                        ui.roomList.visibility = View.GONE
                    } else {
                        ui.txtEmpty.visibility = View.GONE
                        ui.roomList.visibility = View.VISIBLE
                    }
                    ui.roomList.adapter?.notifyDataSetChanged()
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

    private fun showEditRoomDialog(room: Room, position: Int) {
        val input = EditText(this)
        input.setText(room.name)
        input.setPadding(48, 32, 48, 32)

        AlertDialog.Builder(this)
            .setTitle("Edit Room")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter a room name!", Toast.LENGTH_SHORT).show()
                } else {
                    updateRoom(room, name, position)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRoom(room: Room, newName: String, position: Int) {
        val db = Firebase.firestore
        houseId?.let { id ->
            room.id?.let { rid ->
                db.collection("houses").document(id)
                    .collection("rooms").document(rid)
                    .update("name", newName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Room updated! ✅", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun saveRoom(name: String) {
        val db = Firebase.firestore
        val room = Room(name = name)
        houseId?.let { id ->
            db.collection("houses").document(id).collection("rooms")
                .add(room)
                .addOnSuccessListener {
                    Toast.makeText(this, "Room added! 🏠", Toast.LENGTH_SHORT).show()
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
            holder.ui.txtWindowCount.text = "0 Window(s)"
            holder.ui.txtFloorCount.text = "0 Floor Space(s)"
            holder.ui.txtLabourCost.text = "Labour: $200"

            val db = Firebase.firestore
            houseId?.let { hid ->
                room.id?.let { rid ->
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("windows").get()
                        .addOnSuccessListener { w ->
                            holder.ui.txtWindowCount.text = "${w.size()} Window(s)"
                        }
                    db.collection("houses").document(hid)
                        .collection("rooms").document(rid)
                        .collection("floorspaces").get()
                        .addOnSuccessListener { f ->
                            holder.ui.txtFloorCount.text = "${f.size()} Floor Space(s)"
                        }
                }
            }

            holder.ui.checkRoom.isChecked = selectedRooms.contains(room.id) // restore selection
            holder.ui.checkRoom.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    room.id?.let { selectedRooms.add(it) }
                } else {
                    room.id?.let { selectedRooms.remove(it) }
                }
            }

            holder.itemView.setOnClickListener {
                val intent = android.content.Intent(this@HouseDetailActivity, RoomDetailActivity::class.java)
                intent.putExtra("HOUSE_ID", houseId)
                intent.putExtra("ROOM_ID", room.id)
                intent.putExtra("ROOM_NAME", room.name)
                startActivity(intent)
            }

            holder.itemView.setOnLongClickListener { // long press to rename
                showEditRoomDialog(room, position)
                true
            }
        }
    }
}