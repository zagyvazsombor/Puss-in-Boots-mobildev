// DialogShoe.kt
package com.example.shoeregistry

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Egyetlen dialógus CREATE és EDIT módra.
 * - CREATE: mennyiség látszik, brand módosítható.
 * - EDIT: mennyiség rejtve, brand nem módosítható (spinner disabled).
 */
class DialogShoe(
    private val mode: Mode,
    // CREATE módnál: visszaadjuk a létrehozott ShoeEntity-ket
    private val onCreated: (List<ShoeEntity>) -> Unit = {},
    // EDIT módnál: visszaadjuk a frissített ShoeEntity-t
    private val onUpdated: (ShoeEntity) -> Unit = {}
) : DialogFragment() {

    sealed class Mode {
        object CREATE : Mode()
        data class EDIT(val shoe: ShoeEntity) : Mode()
    }

    private fun brandIconRes(brand: String): Int = when (brand) {
        "Nike" -> R.drawable.nike
        "Adidas" -> R.drawable.adidas
        "Puma" -> R.drawable.puma
        "Reebok" -> R.drawable.reebok
        else -> R.drawable.home_screen
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_shoe, null)

        // --- View referenciák ---
        val spinnerSubBrand = view.findViewById<Spinner>(R.id.spinner_subBrand)
        val spinnerBrand = view.findViewById<Spinner>(R.id.spinner_Brand)
        val spinnerSize = view.findViewById<Spinner>(R.id.spinner_Size)
        val spinnerColor = view.findViewById<Spinner>(R.id.spinner_Color)
        val etQuantity = view.findViewById<EditText>(R.id.et_Shoe_Quantity) // csak CREATE-nél

        // --- Adatforrások ---
        val brands = listOf("Nike", "Adidas", "Puma", "Reebok")

        val subBrands = mapOf(
            "Nike" to listOf("Air Jordan", "Air Max", "Dunk", "Cortez"),
            "Adidas" to listOf("Yeezy", "Originals", "Predator", "Ultraboost"),
            "Puma" to listOf("RS-X", "Suede", "Cali", "Future Rider"),
            "Reebok" to listOf("Classic Leather", "Nano", "Club C", "Zig Kinetica"),
        )

        val colors = mapOf(
            "Nike" to listOf("Fekete", "Fehér", "Piros", "Szürke"),
            "Adidas" to listOf("Fehér", "Fekete", "Kék", "Ezüst"),
            "Puma" to listOf("Fekete", "Piros", "Fehér", "Zöld"),
            "Reebok" to listOf("Fehér", "Piros", "Kék", "Szürke"),
        )

        val sizes = listOf(38, 39, 40, 41, 42, 43)

        // --- Adapterek ---
        spinnerBrand.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, brands)
        spinnerSize.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sizes)

        fun setSpinnerSelectionByValue(spinner: Spinner, value: Any) {
            val adapter = spinner.adapter
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i) == value) {
                    spinner.setSelection(i)
                    break
                }
            }
        }

        fun refreshSubBrandAndColor(selectedBrand: String) {
            val subAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                subBrands[selectedBrand] ?: emptyList()
            )
            spinnerSubBrand.adapter = subAdapter

            val colorAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                colors[selectedBrand] ?: emptyList()
            )
            spinnerColor.adapter = colorAdapter
        }

        spinnerBrand.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val b = brands[position]
                refreshSubBrandAndColor(b)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- Mód specifikus UI ---
        when (mode) {
            is Mode.CREATE -> {
                etQuantity.visibility = View.VISIBLE
                spinnerBrand.isEnabled = true

                setSpinnerSelectionByValue(spinnerBrand, brands.first())
                refreshSubBrandAndColor(brands.first())
                setSpinnerSelectionByValue(spinnerSubBrand, subBrands[brands.first()]?.firstOrNull() ?: "")
                setSpinnerSelectionByValue(spinnerColor, colors[brands.first()]?.firstOrNull() ?: "")
                setSpinnerSelectionByValue(spinnerSize, 42)
            }
            is Mode.EDIT -> {
                etQuantity.visibility = View.GONE
                spinnerBrand.isEnabled = false

                val s = mode.shoe
                setSpinnerSelectionByValue(spinnerBrand, s.brand)
                refreshSubBrandAndColor(s.brand)
                setSpinnerSelectionByValue(spinnerSubBrand, s.subBrand)
                setSpinnerSelectionByValue(spinnerColor, s.color)
                setSpinnerSelectionByValue(spinnerSize, s.size)
            }
        }

        val title = when (mode) {
            is Mode.CREATE -> "Cipő hozzáadása"
            is Mode.EDIT   -> "Cipő szerkesztése"
        }

        val positive = when (mode) {
            is Mode.CREATE -> "Mentés"
            is Mode.EDIT   -> "Frissítés"
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positive) { _, _ ->
                val selectedBrand = spinnerBrand.selectedItem.toString()
                val selectedSub = spinnerSubBrand.selectedItem.toString()
                val selectedColor = spinnerColor.selectedItem.toString()
                val selectedSize = spinnerSize.selectedItem as Int
                val imgRes = brandIconRes(selectedBrand)

                when (mode) {
                    is Mode.CREATE -> {
                        val quantity = view.findViewById<EditText>(R.id.et_Shoe_Quantity)
                            .text.toString().toIntOrNull() ?: 1
                        if (quantity <= 0) return@setPositiveButton

                        // Előre kimentjük az appContextet és az adatokat
                        val appCtx = requireContext().applicationContext
                        val listToInsert = List(quantity) {
                            ShoeEntity(
                                subBrand = selectedSub,
                                brand = selectedBrand,
                                size = selectedSize,
                                imageResId = imgRes,
                                color = selectedColor
                            )
                        }

                        lifecycleScope.launch {
                            val created: List<ShoeEntity> = withContext(Dispatchers.IO) {
                                val db = AppDatabase.getDatabase(appCtx)
                                val ids = db.shoeDao().insertAll(listToInsert)
                                ids.mapIndexed { i, id -> listToInsert[i].copy(id = id.toInt()) }
                            }
                            if (!isAdded) return@launch
                            onCreated(created)
                        }
                    }

                    is Mode.EDIT -> {
                        val appCtx = requireContext().applicationContext
                        val updated = mode.shoe.copy(
                            subBrand = selectedSub,
                            brand = mode.shoe.brand, // nem módosítható
                            size = selectedSize,
                            imageResId = brandIconRes(mode.shoe.brand),
                            color = selectedColor
                        )

                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val db = AppDatabase.getDatabase(appCtx)
                                db.shoeDao().updateShoe(updated)
                            }
                            if (!isAdded) return@launch
                            onUpdated(updated)
                        }
                    }
                }
            }
            .setNegativeButton("Mégse", null)
            .create()
    }
}
