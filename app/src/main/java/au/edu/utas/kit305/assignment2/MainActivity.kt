package au.edu.utas.kit305.assignment2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import au.edu.utas.kit305.assignment2.databinding.ActivityMainBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemHouseBinding

const val FIREBASE_TAG = "FirebaseLogging"

class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    // allItems keeps ALL houses from Firebase
    private val allItems = mutableListOf<House>()

    // displayItems is what the RecyclerView shows
    private val displayItems = mutableListOf<House>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Set up RecyclerView with displayItems
        ui.houseList.adapter = HouseAdapter(displayItems)
        ui.houseList.layoutManager = LinearLayoutManager(this)

        // Search bar filtering
        ui.searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase().trim()
                displayItems.clear()
                if (query.isEmpty()) {
                    // Show all items when search is empty
                    displayItems.addAll(allItems)
                } else {
                    // Filter from allItems
                    val filtered = allItems.filter {
                        it.customerName?.lowercase()?.contains(query) == true ||
                                it.address?.lowercase()?.contains(query) == true
                    }
                    displayItems.addAll(filtered)
                }
                // Show empty state if no results
                if (displayItems.isEmpty()) {
                    ui.txtEmpty.visibility = android.view.View.VISIBLE
                } else {
                    ui.txtEmpty.visibility = android.view.View.GONE
                }
                (ui.houseList.adapter as HouseAdapter).notifyDataSetChanged()
            }
        })

        // Connect to Firebase
        val db = Firebase.firestore
        Log.d(FIREBASE_TAG, "Firebase connected: ${db.app.name}")

        // Open Add House screen when button clicked
        ui.btnAddHouse.setOnClickListener {
            val intent = Intent(this, AddHouseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadHouses() {
        val db = Firebase.firestore
        db.collection("houses")
            .get()
            .addOnSuccessListener { result ->
                allItems.clear()
                displayItems.clear()
                for (document in result) {
                    val house = document.toObject(House::class.java)
                    house.id = document.id
                    allItems.add(house)
                }
                // Clear search and show all
                ui.searchBar.text.clear()
                displayItems.addAll(allItems)
                if (displayItems.isEmpty()) {
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

            // Click on house card to open details
            holder.itemView.setOnClickListener {
                val intent = Intent(this@MainActivity, HouseDetailActivity::class.java)
                intent.putExtra("HOUSE_ID", house.id)
                intent.putExtra("HOUSE_NAME", house.customerName)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload from Firebase every time we come back
        loadHouses()
    }
}