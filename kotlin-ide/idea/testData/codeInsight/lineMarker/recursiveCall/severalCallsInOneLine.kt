fun f(a: Int): Int {
    if (a > 0) {
        <lineMarker>f</lineMarker>(a - 1) + f(a + 1)
    }

    return <lineMarker>f</lineMarker>(a)
}