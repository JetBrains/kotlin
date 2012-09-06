package foo

fun getLastFocused(callback:(window:Any?)->Unit) {
    callback(null)
}

fun box(): Boolean {
    var result = "no"
    createTab(true) {
        result = it
    }
    return result == "yes"
}

fun createTab(focusWindow:Boolean, callback:((String)->Unit)?) {
    getLastFocused {
        fun createTab() {
            if (focusWindow && callback != null) {
                callback("yes")
            }
        }

        if (it == null) {
            createTab()
        }
    }
}