package mobi.porquenao.poc.kotlin

import android.app.Application
import com.raizlabs.android.dbflow.config.FlowConfig

import com.raizlabs.android.dbflow.config.FlowManager

class DatabaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FlowManager.init(FlowConfig.Builder(this).build())
    }

    override fun onTerminate() {
        super.onTerminate()
        FlowManager.destroy()
    }
}