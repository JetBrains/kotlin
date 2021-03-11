internal class Test : Base() {
    override fun hashCode(): Int {
        TODO("_root_ide_package_")
    }

    override fun equals(o: Any?): Boolean {
        TODO("_root_ide_package_")
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        return super.clone()
    }

    override fun toString(): String {
        TODO("_root_ide_package_")
    }

    @Throws(Throwable::class)
    override fun finalize() {
        TODO("_root_ide_package_")
    }
}

internal open class Base : Cloneable {
    override fun hashCode(): Int {
        TODO("_root_ide_package_")
    }

    override fun equals(o: Any?): Boolean {
        TODO("_root_ide_package_")
    }

    @Throws(CloneNotSupportedException::class)
    override fun clone(): Any {
        TODO("_root_ide_package_")
    }

    override fun toString(): String {
        TODO("_root_ide_package_")
    }

    @Throws(Throwable::class)
    protected open fun finalize() {
        TODO("_root_ide_package_")
    }
}
