// !CHECK_HIGHLIGHTING

package test

actual open class ExpectedChild : SimpleParent() {
    actual override fun `foo fun`(n: Int) {}
    actual override val `bar fun`: Int get() = 1
}

class ExpectedChildChildJvm : ExpectedChild() {
    override fun `foo fun`(n: Int) {}
    override val `bar fun`: Int get() = 1
}