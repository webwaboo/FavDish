package com.example.favdish.view.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.favdish.view.activities.AddUpdateDishActivity
import com.example.favdish.view.activities.databinding.ItemCustomListBinding
import com.example.favdish.view.fragments.AllDishesFragment

class CustomListItemAdapter(
    private val activity: Activity, //need Activity to work
    private val fragment: Fragment?,
    private val listItems: List<String>, //so we know which list we using
    private val selection: String //so we know what the user has selected
) : RecyclerView.Adapter<CustomListItemAdapter.ViewHolder>() {

    class ViewHolder(view: ItemCustomListBinding): RecyclerView.ViewHolder(view.root){
        val tvText = view.tvText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemCustomListBinding = ItemCustomListBinding.inflate(LayoutInflater.from(activity), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listItems[position]
        holder.tvText.text = item


        holder.itemView.setOnClickListener{
            if(activity is AddUpdateDishActivity){
                activity.selectedListItem(item, selection)
            }
            if (fragment is AllDishesFragment){
                fragment.filterSelection(item)

            }
        }
    }


    override fun getItemCount(): Int {
        return listItems.size
    }
}

