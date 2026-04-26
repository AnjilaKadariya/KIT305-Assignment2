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
    private val allItems = mutableListOf<House>()
    private val displayItems = mutableListOf<House>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.houseList.adapter = HouseAdapter(displayItems)
        ui.houseList.layoutManager = LinearLayoutManager(this)

        ui.searchBar.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase().trim()
                displayItems.clear()
                if (query.isEmpty()) {
                    displayItems.addAll(allItems)
                } else {
                    val filtered = allItems.filter {
                        it.customerName?.lowercase()?.contains(query) == true ||
                                it.address?.lowercase()?.contains(query) == true
                    }
                    displayItems.addAll(filtered)
                }
                if (displayItems.isEmpty()) {
                    ui.txtEmpty.visibility = android.view.View.VISIBLE
                } else {
                    ui.txtEmpty.visibility = android.view.View.GONE
                }
                (ui.houseList.adapter as HouseAdapter).notifyDataSetChanged()
            }
        })

        ui.btnAddHouse.setOnClickListener {
            val intent = Intent(this, AddHouseActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadHouses() {
        val db = Firebase.firestore
        db.collection("houses")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { result, error ->
                if (error != null) {
                    Log.e(FIREBASE_TAG, "Error loading houses", error)
                    return@addSnapshotListener
                }
                allItems.clear()
                displayItems.clear()
                for (document in result!!) {
                    val house = document.toObject(House::class.java)
                    house.id = document.id
                    allItems.add(house)
                }
                val query = ui.searchBar.text.toString().lowercase().trim()
                if (query.isEmpty()) {
                    displayItems.addAll(allItems)
                } else {
                    displayItems.addAll(allItems.filter {
                        it.customerName?.lowercase()?.contains(query) == true ||
                                it.address?.lowercase()?.contains(query) == true
                    })
                }
                if (displayItems.isEmpty()) {
                    ui.txtEmpty.visibility = android.view.View.VISIBLE
                } else {
                    ui.txtEmpty.visibility = android.view.View.GONE
                }
                (ui.houseList.adapter as HouseAdapter).notifyDataSetChanged()
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
            holder.ui.txtPhone.text = house.phone ?: "No phone"
            holder.ui.txtAddress.text = house.address ?: "No address"

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
        loadHouses()

    }
}