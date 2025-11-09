package com.example.shoeregistry

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import androidx.appcompat.widget.SearchView

class MainActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView
    lateinit var shoeAdapter: ShoeAdapter
    // Forráslista (teljes lista). A kereső ettől függetlenül szűrt listát ad az adapternek.
    val shoes = mutableListOf<ShoeEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val themeSwitch = findViewById<SwitchMaterial>(R.id.theme_switch)
        val fab = findViewById<FloatingActionButton>(R.id.fab_Add_Shoe)
        val db = AppDatabase.getDatabase(this)
        val shoeDao = db.shoeDao()
        val searchView = findViewById<SearchView>(R.id.search_Bar)

        // Kereső alapállapot
        searchView.isIconified = false
        searchView.clearFocus()

        // RecyclerView + adapter
        recyclerView = findViewById(R.id.recycler_View)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // FONTOS: olyan ShoeAdapter-t használunk, amelyik támogatja az onEdit callbacket és a replaceAt-et.
        shoeAdapter = ShoeAdapter(shoes) { shoe, indexInAdapter ->
            // Ceruza gomb -> EDIT módú közös dialógus
            val dialog = DialogShoe(
                mode = DialogShoe.Mode.EDIT(shoe),
                onUpdated = { updated ->
                    // 1) Forráslista frissítése ID alapján
                    val posInSource = shoes.indexOfFirst { it.id == updated.id }
                    if (posInSource != -1) {
                        shoes[posInSource] = updated
                    }
                    // 2) Az adapter aktuális (esetleg szűrt) listájának adott elemét is frissítjük
                    shoeAdapter.replaceAt(indexInAdapter, updated)

                    Snackbar.make(recyclerView, "Cipő frissítve", Snackbar.LENGTH_SHORT).show()
                }
            )
            dialog.show(supportFragmentManager, "editShoe")
        }
        recyclerView.adapter = shoeAdapter

        // Kezdeti adatok betöltése Room-ból
        lifecycleScope.launch {
            val shoeEntities = shoeDao.getAll()
            shoes.clear()
            shoes.addAll(shoeEntities.map {
                ShoeEntity(
                    it.id,
                    it.subBrand,
                    it.brand,
                    it.size,
                    it.imageResId,
                    it.color
                )
            })
            shoeAdapter.notifyDataSetChanged()
        }

        // FAB -> CREATE módú közös dialógus (mennyiség itt látszik)
        fab.setOnClickListener {
            val dialog = DialogShoe(
                mode = DialogShoe.Mode.CREATE,
                onCreated = { newShoes ->
                    val start = shoes.size
                    shoes.addAll(newShoes)

                    // Egyszerű megoldás: az adapter teljes listáját frissítjük a forráslistával.
                    // (Ha difft szeretnél, használhatsz DiffUtil-t is.)
                    shoeAdapter.updateList(shoes)

                    recyclerView.scrollToPosition(shoes.size - 1)

                    Snackbar.make(recyclerView, "${newShoes.size} cipő hozzáadva", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            // Visszavonás: töröljük az imént beszúrt tételeket a DB-ből és a listából
                            lifecycleScope.launch {
                                val db = AppDatabase.getDatabase(this@MainActivity)
                                newShoes.forEach { shoe ->
                                    db.shoeDao().delete(
                                        ShoeEntity(
                                            id = shoe.id,
                                            subBrand = shoe.subBrand,
                                            brand = shoe.brand,
                                            size = shoe.size,
                                            imageResId = shoe.imageResId,
                                            color = shoe.color
                                        )
                                    )
                                }
                                shoes.removeAll(newShoes)
                                shoeAdapter.updateList(shoes)
                            }
                        }
                        .show()
                }
            )
            dialog.show(supportFragmentManager, "createShoe")
        }

        // Jobbra húzás -> törlés
        val itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val shoeToRemove = shoes[position]

                    lifecycleScope.launch {
                        val entity = ShoeEntity(
                            id = shoeToRemove.id,
                            subBrand = shoeToRemove.subBrand,
                            brand = shoeToRemove.brand,
                            size = shoeToRemove.size,
                            imageResId = shoeToRemove.imageResId,
                            color = shoeToRemove.color
                        )
                        shoeDao.delete(entity)

                        shoes.removeAt(position)
                        // Ha szűrt lista látszik, a legegyszerűbb minden törlés után újraszűrni a jelenlegi query-re.
                        // Itt most a teljes listát adjuk vissza az adapternek:
                        shoeAdapter.updateList(shoes)
                    }
                }
            })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Light/Dark váltó
        themeSwitch.setOnClickListener {
            val mode = if (themeSwitch.isChecked)
                AppCompatDelegate.MODE_NIGHT_NO
            else
                AppCompatDelegate.MODE_NIGHT_YES

            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Kereső: szűrés a forráslistán, adapternek a szűrt lista megy
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterShoes(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterShoes(newText.orEmpty())
                return true
            }
        })
    }

    // Egyszerű kliens oldali szűrés (forráslista -> szűrt lista az adapterbe)
    private fun filterShoes(query: String) {
        val filtered = shoes.filter { shoe ->
            shoe.subBrand.contains(query, ignoreCase = true) ||
                    shoe.brand.contains(query, ignoreCase = true) ||
                    shoe.color.contains(query, ignoreCase = true) ||
                    shoe.size.toString().contains(query)
        }
        shoeAdapter.updateList(filtered)
    }
}