package mobi.porquenao.poc.kotlin.ui

import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import mobi.porquenao.poc.kotlin.R

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareTaskDescription()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setSupportActionBar(toolbar)
    }

    private fun prepareTaskDescription() {
        if (Build.VERSION.SDK_INT >= 21) {
            if (sTaskDescription == null) {
                val label = getString(R.string.app_name)
                val icon = BitmapFactory.decodeResource(resources, R.drawable.ic_task)
                val colorPrimary = resources.getColor(R.color.app_primary_500)
                sTaskDescription = ActivityManager.TaskDescription(label, icon, colorPrimary)
            }
            setTaskDescription(sTaskDescription)
        }
    }

    companion object {
        private var sTaskDescription: ActivityManager.TaskDescription? = null
    }
}
