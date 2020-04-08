fun f(x : Int) {
    val y = if (x < 0) 1 else when {
        <selection>x - 1</selection> < 1 -> 2
        else -> 3
    }
}