object AAA {
    var x = 42
    var y = 0
    var z = 0
        set(z: Int) {
            Other.z = z
        }
}

internal object Other {
    var z = 0
}
