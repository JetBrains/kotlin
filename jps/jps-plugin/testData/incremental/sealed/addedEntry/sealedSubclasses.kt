internal class BigBar() : IBar {
    override fun foo(): Int {
        return "Bar".hashCode()
    }
}

internal class SmallBar() : IBar {
    override fun foo(): Int {
        return "bar".hashCode()
    }
}
