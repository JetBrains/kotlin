internal open class Base {
    override fun equals(o: Any?): Boolean {
        TODO("_root_ide_package_")
    }
}

internal class X : Base() {
    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }
}
