package org.lsposed.corepatch.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import org.lsposed.corepatch.data.PreferenceData
import org.lsposed.corepatch.ui.PreferenceLayout

class PreferenceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val view: PreferenceLayout = itemView as PreferenceLayout

    fun bind(data: PreferenceData) {
        view.titleView.text = data.title
        if (data.description.isNotEmpty()) view.subtitleView.text = data.description
        view.setOnClickListener {

        }
    }
}