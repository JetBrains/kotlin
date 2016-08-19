package org.example.kotlin.butterknife

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import butterknife.ButterKnife
import butterknife.BindView

class SimpleAdapter(context: Context) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount() = CONTENTS.size
    override fun getItem(position: Int) = CONTENTS[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, v: View?, parent: ViewGroup): View {
        var view = v
        val holder: ViewHolder
        if (view != null) {
            holder = view.tag as ViewHolder
        } else {
            view = inflater.inflate(R.layout.simple_list_item, parent, false)
            holder = ViewHolder(view)
            view!!.tag = holder
        }

        val word = getItem(position)
        holder.word.text = "Word: $word"
        holder.length.text = "Length: ${word.length}"
        holder.position.text = "Position: $position"

        return view
    }

    class ViewHolder(view: View) {
        @BindView(R.id.word)
        lateinit var word: TextView

        @BindView(R.id.length)
        lateinit var length: TextView

        @BindView(R.id.position)
        lateinit var position: TextView

        init {
            ButterKnife.bind(this, view)
        }
    }

    companion object {
        private val CONTENTS = "The quick brown fox jumps over the lazy dog".split(" ")
    }
}
