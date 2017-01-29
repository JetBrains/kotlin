package com.github.frankiesardo.icepick

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.sample.icepick.lib.BaseActivity
import icepick.State

class MainActivity : BaseActivity() {
    @JvmField @State(MyBundler::class)
    var message: String? = null

    lateinit var customView: CustomView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        customView = findViewById(R.id.custom_view) as CustomView
        updateText()
    }

    private fun updateText() {
        val defaultText = if (message == null || baseMessage == null) {
            "Use the menu to add some state"
        } else {
            baseMessage + message
        }
        customView.text = defaultText
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add_state) {
            customView.setBackgroundColorWithAnotherMethod(Color.BLUE)
            customView.setTextColorWithAnotherMethod(Color.WHITE)

            baseMessage = "This state will be automagically "
            message = "saved and restored"
            updateText()
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }
}
