package foo

fun getLastFocused(callback:(window:Any?)->Unit) {
    callback(null)
}

abstract class TabService(val ctorParam:String) {
    abstract fun createTab(focusWindow:Boolean, callback: (tabs: String)->Unit)
}

abstract class PageManager(val tabService: TabService)

class ChromePageManager(val expected: String): PageManager(object : TabService(expected) {
    private fun postProcessCreatedTab() {
    }

    override fun createTab(focusWindow: Boolean, callback: (tabs: String)->Unit) {
        getLastFocused {
            fun createTab() {
                if (focusWindow) {
                    postProcessCreatedTab()
                    callback(expected)
                }
            }

            if (it == null) {
                getAll {
                    createTab()
                }
            }
        }
    }
}) {

}

fun box(): Boolean {
    var result = ""
    val tabService = ChromePageManager("result").tabService
    tabService.createTab(true) {
        result = it
    }
    return result == "result" && tabService.ctorParam == "result"
}

fun getAll(callback:(()->Unit)) {
    callback()
}