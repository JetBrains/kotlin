fun f(a: Int) {
    run2(fun () {
        <lineMarker>f</lineMarker>(a - 1)
    })
}

fun ff(a: Int) {
    run1 {
        ff(a - 1)
    }
}

inline fun <T> run1(noinline f: () -> T): T { }

inline fun <T> run2(f: () -> T): T { }

