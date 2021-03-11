open class <lineMarker>C</lineMarker>(
        open val <lineMarker descr="<html><body>Is overridden in <br/>&nbsp;&nbsp;&nbsp;&nbsp;D</body></html>">s</lineMarker>: String
) {

}


class D : C("") {
    override val <lineMarker>s</lineMarker>: String get() = "q"
}

/*
LINEMARKER: <html><body>Is overridden in <br/>&nbsp;&nbsp;&nbsp;&nbsp;D</body></html>
TARGETS:
PrimaryConstructorOpen.kt
    override val <1>s: String get() = "q"
*/
