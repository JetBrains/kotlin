package org.example.kotlin.butterknife

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import android.widget.Toast.LENGTH_SHORT
import butterknife.*

class SimpleActivity : Activity() {

    @BindView(R.id.title)
    lateinit var title: TextView

    @BindView(R.id.subtitle)
    lateinit var subtitle: TextView

    @BindView(R.id.hello)
    lateinit var hello: Button

    @BindView(R.id.list_of_things)
    lateinit var listOfThings: ListView

    @BindView(R.id.footer)
    lateinit var footer: TextView

    @BindViews(R.id.title, R.id.subtitle, R.id.hello)
    lateinit var headerViews: MutableList<View>

    @JvmField
    @BindColor(R.color.blue)
    var titleTextColor: Int = 0

    private lateinit var adapter: SimpleAdapter

    @OnClick(R.id.hello)
    fun sayHello() {
        Toast.makeText(this, "Hello, views!", LENGTH_SHORT).show()
        ButterKnife.apply(headerViews.toList(), ALPHA_FADE)
    }

    @OnLongClick(R.id.hello)
    fun sayGetOffMe(): Boolean {
        Toast.makeText(this, "Let go of me!", LENGTH_SHORT).show()
        return true
    }

    @OnItemClick(R.id.list_of_things)
    fun onItemClick(position: Int) {
        Toast.makeText(this, "You clicked: " + adapter.getItem(position), LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_activity)
        ButterKnife.bind(this)

        title.text = "Butter Knife"
        subtitle.text = "Field and method binding for Android views."
        footer.text = "by Jake Wharton"
        hello.text = "Say Hello"

        title.setTextColor(titleTextColor)

        adapter = SimpleAdapter(this)
        listOfThings.adapter = adapter
    }

    companion object {
        private val ALPHA_FADE = ButterKnife.Action<View> { view, index ->
            with (AlphaAnimation(0f, 1f)) {
                fillBefore = true
                duration = 500
                startOffset = (index * 100).toLong()
                view.startAnimation(this)
            }
        }
    }
}
