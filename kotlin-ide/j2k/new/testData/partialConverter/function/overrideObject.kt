internal class X : Cloneable {
    override fun hashCode(): Int {
        TODO("_root_ide_package_")
    }

    override fun equals(o: Any?): Boolean {
        return super.equals(o)
    }

    override fun toString(): String {
        TODO("_root_ide_package_")
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        TODO("_root_ide_package_")
    }
}

internal class Y : Thread() {
    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        TODO("_root_ide_package_")
    }
}
