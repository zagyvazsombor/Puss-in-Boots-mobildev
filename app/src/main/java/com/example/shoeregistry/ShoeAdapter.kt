// ShoeAdapter.kt
package com.example.shoeregistry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShoeAdapter(
    // Az adapter a listát kapja (szűrt vagy teljes); szükség esetén új listát állítunk be.
    private var shoes: List<ShoeEntity>,
    // EDIT ikon (ceruza) callback: visszaadjuk az elemet és az aktuális adapter pozíciót
    private val onEdit: (ShoeEntity, Int) -> Unit
) : RecyclerView.Adapter<ShoeAdapter.ShoeViewHolder>() {

    class ShoeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_Shoe_Image)
        val subBrand: TextView = itemView.findViewById(R.id.tv_Shoe_subBrand)
        val brand: TextView = itemView.findViewById(R.id.tv_Shoe_Brand)
        val size: TextView = itemView.findViewById(R.id.tv_Shoe_Size)
        val color: TextView = itemView.findViewById(R.id.tv_Shoe_Color)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_Edit)
    }

    /** Keresés/szűrés után új lista beállítása */
    fun updateList(newList: List<ShoeEntity>) {
        shoes = newList
        notifyDataSetChanged()
    }

    /** Egy konkrét sor cseréje az *aktuális* adapter-listában */
    fun replaceAt(index: Int, updated: ShoeEntity) {
        if (index in shoes.indices) {
            val mutable = shoes.toMutableList()
            mutable[index] = updated
            shoes = mutable
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ShoeViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_shoe, parent, false)
        )

    override fun onBindViewHolder(holder: ShoeViewHolder, position: Int) {
        val shoe = shoes[position]
        holder.subBrand.text = shoe.subBrand
        holder.brand.text = shoe.brand
        holder.size.text = "Méret: ${shoe.size}"
        holder.image.setImageResource(shoe.imageResId)
        holder.color.text = shoe.color

        holder.btnEdit.setOnClickListener {
            val idx = holder.bindingAdapterPosition
            if (idx != RecyclerView.NO_POSITION) {
                onEdit(shoe, idx)
            }
        }
    }

    override fun getItemCount() = shoes.size
}
