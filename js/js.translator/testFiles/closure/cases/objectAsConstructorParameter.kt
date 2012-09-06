package foo

fun getLastFocused(callback: (window: Any?)->Unit) {
    callback(null)
}

abstract class TabService(val ctorParam:String) {
    abstract fun query(callback: (tabs: String)->Unit)
}

abstract class PageManager(val tabService: TabService)

class ChromePageManager(val expected:String): PageManager(object : TabService(expected) {
    override fun query(callback: (tabs: String)->Unit) {
        getLastFocused {
            callback(expected)
        }
    }
}) {

}

fun box(): Boolean {
    var result = ""
    val tabService = ChromePageManager("result").tabService
    tabService.query {
       result = it
    }
    return result == "result" && tabService.ctorParam == "result"
}