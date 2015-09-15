object AAA {
    var x = 42
    var y = 0
    val z = 0

    fun setZ(z: Int) {
        Other.z = z
    }
}

internal object Other {
    var z = 0
}
