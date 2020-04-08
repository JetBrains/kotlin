class A {
    var x = 0
        set(value) {
            <lineMarker descr="Recursive call">x</lineMarker>++
            <lineMarker descr="Recursive call">x</lineMarker>+=1
            <lineMarker descr="Recursive call">x</lineMarker> = value
        }

    var y = 0
        get() {
            println("$<lineMarker descr="Recursive call">y</lineMarker>")
            <lineMarker descr="Recursive call">y</lineMarker>++
            <lineMarker descr="Recursive call">y</lineMarker> += 1
            return <lineMarker descr="Recursive call">y</lineMarker>
        }

    var z = 0
        get() = <lineMarker descr="Recursive call">z</lineMarker>

    var field = 0
        get() {
            return if (field != 0) field else -1
        }
        set(value) {
            if (value >= 0) field = value
        }
}