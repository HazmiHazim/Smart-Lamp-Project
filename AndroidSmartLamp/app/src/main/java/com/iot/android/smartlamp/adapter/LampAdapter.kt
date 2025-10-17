package com.iot.android.smartlamp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iot.android.smartlamp.R
import com.iot.android.smartlamp.model.Lamp

class LampAdapter(
    private val lampList : MutableList<Lamp>,
    private val onLampCardClicked : (Lamp) -> Unit
    ) : RecyclerView.Adapter<LampAdapter.ViewHolder>() {

    class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val lampName : TextView = view.findViewById(R.id.card_lamp_name)
        val lampModel : TextView = view.findViewById(R.id.card_lamp_model)
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
        holder.lampModel.text = lamp.model
        holder.lampColour.text = lamp.colour

        if (lamp.state) {
            holder.lampState.text = "ON"
            holder.lampState.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.green))
        } else {
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
        lampList.clear()
        lampList.addAll(newList)
        notifyDataSetChanged()
    }
}