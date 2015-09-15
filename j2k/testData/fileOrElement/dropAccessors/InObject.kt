object AAA {
    var x: Int = 42
    var y: Int = 0
    val z: Int = 0

    fun setZ(z: Int) {
        Other.z = z
    }
}

internal object Other {
    var z: Int = 0
}
