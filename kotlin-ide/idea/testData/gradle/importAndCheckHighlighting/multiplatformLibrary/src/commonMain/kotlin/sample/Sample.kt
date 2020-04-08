package sample

expect class <lineMarker descr="Has actuals in JS, JVM">Sample</lineMarker>() {
    fun <lineMarker descr="Has actuals in JS, JVM">checkMe</lineMarker>(): Int
}

expect object <lineMarker descr="Has actuals in JS, JVM">Platform</lineMarker> {
    val <lineMarker descr="Has actuals in JS, JVM">name</lineMarker>: String
}

fun hello(): String = "Hello from ${Platform.name}"