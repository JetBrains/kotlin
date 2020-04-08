interface Application {
    fun put(testKey: String, s: String)
}

abstract class ApplicationModule {
    abstract fun install(<caret>application: Application)
}

class AppTest {
    class ApplicationLoaderTestApplicationFeatureWithEnvironment() : ApplicationModule() {
        override fun install(application: Application) {
            application.put(TestKey, "2")
        }
    }

    companion object {
        val TestKey = ""
    }
}