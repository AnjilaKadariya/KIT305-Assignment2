package au.edu.utas.kit305.assignment2

import android.os.Bundle
import android.view.ViewGroup
import android.view.View
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityRoomDetailBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemWindowBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemFloorSpaceBinding

class RoomDetailActivity : AppCompatActivity() {

    private lateinit var ui: ActivityRoomDetailBinding
    private var houseId: String? = null
    private var roomId: String? = null
    private val windows = mutableListOf<Window>()
    private val floorSpaces = mutableListOf<FloorSpace>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityRoomDetailBinding.inflate(layoutInflater)
        setContentView(ui.root)

        houseId = intent.getStringExtra("HOUSE_ID")
        roomId = intent.getStringExtra("ROOM_ID")
        val roomName = intent.getStringExtra("ROOM_NAME")

        // Toolbar
        ui.toolbar.title = roomName ?: "Room"
        ui.toolbar.setNavigationOnClickListener { finish() }

        // Set up RecyclerViews
        ui.windowList.layoutManager = LinearLayoutManager(this)
        ui.windowList.adapter = WindowAdapter(windows)

        ui.floorSpaceList.layoutManager = LinearLayoutManager(this)
        ui.floorSpaceList.adapter = FloorSpaceAdapter(floorSpaces)

        // Load data
        loadWindows()
        loadFloorSpaces()

        // Add window button
        ui.btnAddWindow.setOnClickListener {
           val intent = android.content.Intent(this, AddWindowActivity::class.java)
            intent.putExtra("HOUSE_ID", houseId)
            intent.putExtra("ROOM_ID", roomId)
            startActivity(intent)
        }

        // Add floor space button
        ui.btnAddFloorSpace.setOnClickListener {
           val intent = android.content.Intent(this, AddFloorSpaceActivity::class.java)
            intent.putExtra("HOUSE_ID", houseId)
            intent.putExtra("ROOM_ID", roomId)
            startActivity(intent)
        }
    }

    private fun loadWindows() {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("windows")
                    .get()
                    .addOnSuccessListener { result ->
                        windows.clear()
                        for (doc in result) {
                            val window = doc.toObject(Window::class.java)
                            window.id = doc.id
                            windows.add(window)
                        }
                        ui.txtNoWindows.visibility = if (windows.isEmpty()) View.VISIBLE else View.GONE
                        (ui.windowList.adapter as WindowAdapter).notifyDataSetChanged()
                    }
            }
        }
    }

    private fun loadFloorSpaces() {
        val db = Firebase.firestore
        houseId?.let { hid ->
            roomId?.let { rid ->
                db.collection("houses").document(hid)
                    .collection("rooms").document(rid)
                    .collection("floorspaces")
                    .get()
                    .addOnSuccessListener { result ->
                        floorSpaces.clear()
                        for (doc in result) {
                            val fs = doc.toObject(FloorSpace::class.java)
                            fs.id = doc.id
                            floorSpaces.add(fs)
                        }
                        ui.txtNoFloorSpaces.visibility = if (floorSpaces.isEmpty()) View.VISIBLE else View.GONE
                        (ui.floorSpaceList.adapter as FloorSpaceAdapter).notifyDataSetChanged()
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadWindows()
        loadFloorSpaces()
    }

    inner class WindowHolder(var ui: ListItemWindowBinding) : RecyclerView.ViewHolder(ui.root)
    inner class WindowAdapter(private val list: MutableList<Window>) : RecyclerView.Adapter<WindowHolder>() {
        override fun getItemCount() = list.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WindowHolder {
            val ui = ListItemWindowBinding.inflate(layoutInflater, parent, false)
            return WindowHolder(ui)
        }
        override fun onBindViewHolder(holder: WindowHolder, position: Int) {
            val window = list[position]
            holder.ui.txtWindowName.text = window.name ?: "No name"
            holder.ui.txtWindowSize.text = "${window.width} x ${window.height} mm"
        }
    }

    inner class FloorSpaceHolder(var ui: ListItemFloorSpaceBinding) : RecyclerView.ViewHolder(ui.root)
    inner class FloorSpaceAdapter(private val list: MutableList<FloorSpace>) : RecyclerView.Adapter<FloorSpaceHolder>() {
        override fun getItemCount() = list.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FloorSpaceHolder {
            val ui = ListItemFloorSpaceBinding.inflate(layoutInflater, parent, false)
            return FloorSpaceHolder(ui)
        }
        override fun onBindViewHolder(holder: FloorSpaceHolder, position: Int) {
            val fs = list[position]
            holder.ui.txtFloorSpaceName.text = fs.name ?: "No name"
            holder.ui.txtFloorSpaceSize.text = "${fs.width} x ${fs.depth} m"
        }
    }
}