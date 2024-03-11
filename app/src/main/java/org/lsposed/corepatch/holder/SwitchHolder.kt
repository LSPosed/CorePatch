package org.lsposed.corepatch.holder

import android.support.v7.widget.RecyclerView
import android.view.View
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.data.SwitchData
import org.lsposed.corepatch.ui.CustomSwitchLayout

class SwitchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val view: CustomSwitchLayout = itemView as CustomSwitchLayout

    fun bind(data: SwitchData) {
        view.titleView.text = data.title
        view.subtitleView.text = data.description

        view.setOnCheckListener {}
        view.switchView.isChecked = Config.getConfig(data.key)
        view.setOnCheckListener { isChecked ->
            Config.setConfig(data.key, isChecked)
        }
    }
}