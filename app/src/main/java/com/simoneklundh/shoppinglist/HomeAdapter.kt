package com.simoneklundh.shoppinglist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class HomeAdapter(
    private val onClick: (ShoppingListMeta) -> Unit,
    private val onLongClick: (ShoppingListMeta) -> Unit,
    private val remainingItemCount: (ShoppingListMeta) -> Int,
) : RecyclerView.Adapter<HomeAdapter.ListViewHolder>() {

    private var lists: MutableList<ShoppingListMeta> = mutableListOf()

    fun submitList(newLists: List<ShoppingListMeta>) {
        val diff = DiffUtil.calculateDiff(ListDiff(lists, newLists))
        lists = newLists.toMutableList()
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = lists.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_card, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(lists[position])
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.listName)
        private val countText: TextView = itemView.findViewById(R.id.listItemCount)

        fun bind(list: ShoppingListMeta) {
            nameText.text = list.name
            val count = remainingItemCount(list)
            countText.text = if (count == 0) {
                itemView.resources.getString(R.string.list_empty)
            } else {
                itemView.resources.getQuantityString(R.plurals.list_item_count, count, count)
            }
            itemView.setOnClickListener { onClick(list) }
            itemView.setOnLongClickListener {
                onLongClick(list)
                true
            }
        }
    }

    private class ListDiff(
        private val old: List<ShoppingListMeta>,
        private val new: List<ShoppingListMeta>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition].id == new[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] == new[newItemPosition]
    }
}
