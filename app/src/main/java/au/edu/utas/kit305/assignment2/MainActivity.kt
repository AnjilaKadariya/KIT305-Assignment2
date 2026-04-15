package au.edu.utas.kit305.assignment2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityMainBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemHouseBinding

// Tag for Firebase logging
const val FIREBASE_TAG = "FirebaseLogging"

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding
    private val items = mutableListOf<House>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Set up RecyclerView
        ui.houseList.adapter = HouseAdapter(items)
        ui.houseList.layoutManager = LinearLayoutManager(this)

        // Connect to Firebase
        val db = Firebase.firestore
        Log.d(FIREBASE_TAG, "Firebase connected: ${db.app.name}")

        // Load houses from Firebase
        db.collection("houses")
            .get()
            .addOnSuccessListener { result ->
                items.clear()
                for (document in result) {
                    val house = document.toObject(House::class.java)
                    house.id = document.id
                    items.add(house)
                }
                // Show empty state if no projects
                if (items.isEmpty()) {
                    ui.txtEmpty.visibility = android.view.View.VISIBLE
                } else {
                    ui.txtEmpty.visibility = android.view.View.GONE
                }
                (ui.houseList.adapter as HouseAdapter).notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.e(FIREBASE_TAG, "Error loading houses", it)
            }
    }

    inner class HouseHolder(var ui: ListItemHouseBinding) :
        RecyclerView.ViewHolder(ui.root) {}

    inner class HouseAdapter(private val houses: MutableList<House>) :
        RecyclerView.Adapter<HouseHolder>() {

        override fun getItemCount(): Int = houses.size

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): HouseHolder {
            val ui = ListItemHouseBinding.inflate(layoutInflater, parent, false)
            return HouseHolder(ui)
        }

        override fun onBindViewHolder(holder: HouseHolder, position: Int) {
            val house = houses[position]
            holder.ui.txtCustomerName.text = house.customerName ?: "No name"
            holder.ui.txtAddress.text = house.address ?: "No address"
        }
    }

    override fun onResume() {
        super.onResume()
        ui.houseList.adapter?.notifyDataSetChanged()
    }
}