import android.app.Application
import android.content.Context

object AndroidMain {
    operator fun invoke(context: Context): AndroidMain {
        context.applicationContext
        return this
    }

    operator fun invoke(): AndroidMain {
        CommonMain.invoke()
        return AndroidMain
    }
}
