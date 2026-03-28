package com.iot.android.smartlamp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.model.Lamp

class LampAdapter(
    private var lampList : List<Lamp>,
    private val onLampCardClicked : (Lamp) -> Unit
    ) : RecyclerView.Adapter<LampAdapter.ViewHolder>() {

    class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val lampImage : ImageView = view.findViewById(R.id.card_lamp_image)
        val lampName : TextView = view.findViewById(R.id.card_lamp_name)
        val lampState : TextView = view.findViewById(R.id.card_lamp_state)
        val lampColour : TextView = view.findViewById(R.id.card_lamp_colour)
        val cardDetailNav : LinearLayout = view.findViewById(R.id.lamp_card_detail_nav)
    }

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.lamp_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder : ViewHolder, position : Int) {
        val lamp = lampList[position]
        holder.lampName.text = lamp.name
        holder.lampColour.text = if (lamp.colour.isNotEmpty()) lamp.colour else "N/A"

        if (lamp.state) {
            holder.lampImage.setImageResource(R.drawable.bulb_on)
            holder.lampState.text = "ON"
            holder.lampState.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        } else {
            holder.lampImage.setImageResource(R.drawable.bulb_off)
            holder.lampState.text = "OFF"
            holder.lampState.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.red))
        }

        holder.cardDetailNav.setOnClickListener {
            onLampCardClicked(lamp)
        }
    }

    override fun getItemCount() : Int {
        return lampList.size
    }

    fun updateList(newList : List<Lamp>) {
        val diffResult = DiffUtil.calculateDiff(LampDiffCallback(lampList, newList))
        lampList = newList.toList()
        diffResult.dispatchUpdatesTo(this)
    }

    private class LampDiffCallback(
        private val oldList: List<Lamp>,
        private val newList: List<Lamp>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos].id == newList[newPos].id
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }
    }
}
