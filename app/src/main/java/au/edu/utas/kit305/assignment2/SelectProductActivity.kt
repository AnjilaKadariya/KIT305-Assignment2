package au.edu.utas.kit305.assignment2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.edu.utas.kit305.assignment2.databinding.ActivitySelectProductBinding
import au.edu.utas.kit305.assignment2.databinding.ListItemProductBinding

data class Product(
    val id: String,
    val name: String,
    val type: String,
    val pricePerM2: Double,
    val description: String,
    val labourCost: Double,
    val colours: List<String>,
    val imageUrl: String,
    val minWidth: Double,
    val minHeight: Double,
    val minDepth: Double
)

class SelectProductActivity : AppCompatActivity() {

    private lateinit var ui: ActivitySelectProductBinding
    private var category: String = "window"
    private var windowWidth: Float = 0f
    private var windowHeight: Float = 0f
    private var floorWidth: Float = 0f
    private var floorDepth: Float = 0f

    private val allProducts = mutableListOf<Product>()
    private val filteredProducts = mutableListOf<Product>()
    private var selectedProduct: Product? = null
    private var selectedColour: String = ""
    private var spinnerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySelectProductBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.toolbar.setNavigationOnClickListener { finish() }

        category = intent.getStringExtra("category") ?: "window"
        Log.d("PRODUCTS", "Received category: $category")
        windowWidth = intent.getFloatExtra("WINDOW_WIDTH", 0f)
        windowHeight = intent.getFloatExtra("WINDOW_HEIGHT", 0f)
        floorWidth = intent.getFloatExtra("FLOOR_WIDTH", 0f)
        floorDepth = intent.getFloatExtra("FLOOR_DEPTH", 0f)

        val productTypes = if (category == "window") {
            arrayOf("All Types", "Blind", "Curtain", "Shutter")
        } else {
            arrayOf("All Types", "Carpet", "Timber", "Vinyl", "Rubber", "Concrete", "Cork", "Loop")
        }

        val typeAdapter = ArrayAdapter(this, R.layout.spinner_item, productTypes)
        typeAdapter.setDropDownViewResource(R.layout.spinner_item)
        ui.spinnerProductType.adapter = typeAdapter

        ui.productList.layoutManager = LinearLayoutManager(this)
        ui.productList.adapter = ProductAdapter(filteredProducts)

        fetchProducts()

        ui.spinnerProductType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!spinnerReady) { spinnerReady = true; return }
                updateColourSpinnerForType()
                filterProducts()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        ui.spinnerColour.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (!spinnerReady) return
                selectedColour = ui.spinnerColour.selectedItem.toString()
                filterProducts()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        ui.btnSave.setOnClickListener {
            if (selectedProduct == null) {
                Toast.makeText(this, "Please select a product!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val result = Intent()
            result.putExtra("PRODUCT_NAME", selectedProduct!!.name)
            result.putExtra("PRODUCT_PRICE_PER_M2", selectedProduct!!.pricePerM2)
            result.putExtra("PRODUCT_LABOUR", selectedProduct!!.labourCost)
            result.putExtra("PRODUCT_COLOUR", selectedColour)
            result.putExtra("PRODUCT_TYPE", selectedProduct!!.type)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun fetchProducts() {
        ui.progressBar.visibility = View.VISIBLE
        Thread {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url("https://utasbot.dev/kit305_2026/product")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                Log.d("PRODUCTS", "Response code: ${response.code}")
                Log.d("PRODUCTS", "Body length: ${body.length}")

                val jsonObject = org.json.JSONObject(body)
                val jsonArray = jsonObject.getJSONArray("data")
                Log.d("PRODUCTS", "Products: ${jsonArray.length()}")

                val products = mutableListOf<Product>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val coloursArray = obj.optJSONArray("variants")
                    val colours = mutableListOf<String>()
                    if (coloursArray != null) {
                        for (j in 0 until coloursArray.length()) {
                            colours.add(coloursArray.getString(j))
                        }
                    }
                    products.add(Product(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        type = obj.optString("category", ""),
                        pricePerM2 = obj.optDouble("price_per_sqm", 0.0),
                        description = obj.optString("description", ""),
                        labourCost = obj.optDouble("labourCost", 0.0),
                        colours = colours,
                        imageUrl = obj.optString("imageUrl", ""),
                        minWidth = obj.optDouble("min_width", 0.0),
                        minHeight = obj.optDouble("min_height", 0.0),
                        minDepth = 0.0
                    ))
                }

                if (products.isNotEmpty()) {
                    Log.d("PRODUCTS", "First product category: ${products[0].type}")
                }

                runOnUiThread {
                    ui.progressBar.visibility = View.GONE
                    allProducts.clear()
                    allProducts.addAll(products)
                    updateColourSpinner()
                    filterProducts()
                    Toast.makeText(this, "Loaded ${products.size} products!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("PRODUCTS", "Error: ${e.message}", e)
                runOnUiThread {
                    ui.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun updateColourSpinner() {
        val colours = mutableSetOf<String>()
        colours.add("All Colours")
        allProducts
            .filter { it.type.equals(category, ignoreCase = true) }
            .forEach { colours.addAll(it.colours) }

        val colourAdapter = ArrayAdapter(this, R.layout.spinner_item, colours.toList())
        colourAdapter.setDropDownViewResource(R.layout.spinner_item)
        ui.spinnerColour.adapter = colourAdapter
    }

    private fun updateColourSpinnerForType() {
        val colours = mutableSetOf<String>()
        colours.add("All Colours")
        for (product in allProducts) {
            if (!product.type.equals(category, ignoreCase = true)) continue
            colours.addAll(product.colours)
        }
        val colourAdapter = ArrayAdapter(this, R.layout.spinner_item, colours.toList())
        colourAdapter.setDropDownViewResource(R.layout.spinner_item)
        ui.spinnerColour.adapter = colourAdapter
    }

    private fun filterProducts() {
        val selectedType = ui.spinnerProductType.selectedItem?.toString() ?: "All Types"
        val selectedColour = ui.spinnerColour.selectedItem?.toString() ?: "All Colours"

        filteredProducts.clear()
        for (product in allProducts) {
            if (!product.type.equals(category, ignoreCase = true)) continue
            if (selectedColour != "All Colours" && !product.colours.any { it.equals(selectedColour, ignoreCase = true) }) continue
            filteredProducts.add(product)
        }
        Log.d("PRODUCTS", "filterProducts: ${filteredProducts.size} products after filter")
        ui.productList.adapter?.notifyDataSetChanged()
    }

    private fun meetsConstraints(product: Product): Pair<Boolean, String> {
        if (category == "window") {
            if (product.minWidth > 0 && windowWidth < product.minWidth) {
                return Pair(false, "Requires min width ${product.minWidth.toInt()}mm, your window is ${windowWidth.toInt()}mm")
            }
            if (product.minHeight > 0 && windowHeight < product.minHeight) {
                return Pair(false, "Requires min height ${product.minHeight.toInt()}mm, your window is ${windowHeight.toInt()}mm")
            }
        } else {
            if (product.minWidth > 0 && floorWidth < product.minWidth) {
                return Pair(false, "Requires min width ${product.minWidth}m, your floor is ${floorWidth}m")
            }
            if (product.minDepth > 0 && floorDepth < product.minDepth) {
                return Pair(false, "Requires min depth ${product.minDepth}m, your floor is ${floorDepth}m")
            }
        }
        return Pair(true, "")
    }

    inner class ProductHolder(var ui: ListItemProductBinding) : RecyclerView.ViewHolder(ui.root)

    inner class ProductAdapter(private val list: MutableList<Product>) : RecyclerView.Adapter<ProductHolder>() {
        override fun getItemCount() = list.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductHolder {
            val ui = ListItemProductBinding.inflate(layoutInflater, parent, false)
            return ProductHolder(ui)
        }

        override fun onBindViewHolder(holder: ProductHolder, position: Int) {
            val product = list[position]
            val (meets, errorMsg) = meetsConstraints(product)

            holder.ui.txtProductName.text = product.name
            holder.ui.txtPrice.text = "$${product.pricePerM2}/m²"
            holder.ui.txtDescription.text = product.description

            com.bumptech.glide.Glide.with(this@SelectProductActivity)
                .load(product.imageUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(holder.ui.imgProduct)

            if (!meets) {
                holder.ui.root.alpha = 0.5f
                holder.ui.txtConstraintError.visibility = View.VISIBLE
                holder.ui.txtConstraintError.text = errorMsg
            } else {
                holder.ui.root.alpha = 1.0f
                holder.ui.txtConstraintError.visibility = View.GONE
            }

            holder.ui.checkProduct.isChecked = selectedProduct?.id == product.id

            holder.itemView.setOnClickListener {
                if (!meets) {
                    Toast.makeText(this@SelectProductActivity, errorMsg, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                selectedProduct = product
                notifyDataSetChanged()
            }
        }
    }
}
