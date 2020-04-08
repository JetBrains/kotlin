package test

open class <lineMarker>SimpleParent</lineMarker> {
    open fun <lineMarker>foo</lineMarker>(n: Int) {}
    open val <lineMarker>bar</lineMarker>: Int get() = 1
}

expect open class <lineMarker><lineMarker>ExpectedChild</lineMarker></lineMarker> : SimpleParent {
    override fun <lineMarker><lineMarker>foo</lineMarker></lineMarker>(n: Int)
    override val <lineMarker><lineMarker>bar</lineMarker></lineMarker>: Int
}

class ExpectedChildChild : ExpectedChild() {
    override fun <lineMarker>foo</lineMarker>(n: Int) {}
    override val <lineMarker>bar</lineMarker>: Int get() = 1
}

class SimpleChild : SimpleParent() {
    override fun <lineMarker>foo</lineMarker>(n: Int) {}
    override val <lineMarker>bar</lineMarker>: Int get() = 1
}