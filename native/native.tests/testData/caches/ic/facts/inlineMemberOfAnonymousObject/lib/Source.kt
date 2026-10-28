interface Source {
    val size: Int
}

fun source(): Source = object : Source {
    private inline val self get() = this
    val base: Int = 1
    override val size: Int get() = self.base
}
