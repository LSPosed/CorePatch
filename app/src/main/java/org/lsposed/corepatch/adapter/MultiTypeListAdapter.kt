package org.lsposed.corepatch.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.data.PreferenceData
import org.lsposed.corepatch.data.SwitchData
import org.lsposed.corepatch.ui.CustomSwitchLayout

class MultiTypeListAdapter(private val dataSet: ArrayList<*>) : BaseAdapter() {
    override fun getCount(): Int {
        return dataSet.size
    }

    override fun getItem(position: Int): Any {
        return dataSet[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        if (convertView != null) return convertView
        val context = parent!!.context
        when (val data = dataSet[position]) {
            is SwitchData -> {
                val view = CustomSwitchLayout(context)
                view.titleView.text = data.title
                view.subtitleView.text = data.description

                view.setOnCheckListener {}
                view.switchView.isChecked = Config.getConfig(data.key)
                view.setOnCheckListener { isChecked ->
                    Config.setConfig(data.key, isChecked)
                }
                return view
            }

            is PreferenceData -> {
                val view = CustomSwitchLayout(context)
                view.titleView.text = data.title
                if (data.description.isNotEmpty()) view.subtitleView.text = data.description
                view.setOnClickListener {

                }
                return view
            }
        }
        return View(context)
    }
}