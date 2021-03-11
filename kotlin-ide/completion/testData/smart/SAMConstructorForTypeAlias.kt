
typealias TaRunnable = Runnable


fun usesRunnable(runnable: Runnable) {

}

fun usage() {
    usesRunnable(<caret>)
}

// EXIST: {"lookupString":"TaRunnable","tailText":" {...} (function: () -> Unit) (<root>)","typeText":"Runnable"}