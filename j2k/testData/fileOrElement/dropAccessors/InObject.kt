public object AAA {
    public var x: Int = 42
    public var y: Int = 0
    public val z: Int = 0

    public fun setZ(z: Int) {
        Other.z = z
    }
}

object Other {
    public var z: Int = 0
}
