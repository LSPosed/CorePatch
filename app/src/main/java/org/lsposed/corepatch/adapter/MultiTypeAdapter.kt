package org.lsposed.corepatch.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import org.lsposed.corepatch.data.PreferenceData
import org.lsposed.corepatch.data.SwitchData
import org.lsposed.corepatch.holder.PreferenceHolder
import org.lsposed.corepatch.holder.SwitchHolder
import org.lsposed.corepatch.ui.CustomSwitchLayout
import org.lsposed.corepatch.ui.PreferenceLayout

class MultiTypeAdapter(private val dataSet: ArrayList<*>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val VIEW_TYPE_SWITCH = 0
        const val VIEW_TYPE_PREFERENCE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (dataSet[position]) {
            is SwitchData -> VIEW_TYPE_SWITCH
            is PreferenceData -> VIEW_TYPE_PREFERENCE
            else -> -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SWITCH -> SwitchHolder(CustomSwitchLayout(parent.context))
            VIEW_TYPE_PREFERENCE -> PreferenceHolder(PreferenceLayout(parent.context))
            else -> throw Exception("unknown view type")
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_SWITCH -> {
                val switchHolder = holder as SwitchHolder
                switchHolder.bind(dataSet[position] as SwitchData)
            }

            VIEW_TYPE_PREFERENCE -> {
                val preferenceHolder = holder as PreferenceHolder
                preferenceHolder.bind(dataSet[position] as PreferenceData)
            }
        }
    }

}