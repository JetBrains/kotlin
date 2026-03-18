package cases.protected

public abstract class PublicAbstractClass protected constructor() {
    protected abstract val protectedVal: Int
    protected abstract var protectedVar: Any?

    protected abstract fun protectedFun()
}


class FinalClass {
    protected fun foo() {}
}

abstract class AbstractClass {
    protected fun impl() {}
    protected open fun open() {}
    protected abstract fun abstract()
}

internal abstract class InternalAbstractClass {
    protected fun impl() {}
    protected open fun open() {}
    protected abstract fun abstract()
}

@PublishedApi
internal abstract class PublishedAbstractClass {
    protected fun impl() {}
    protected open fun open() {}
    protected abstract fun abstract()
}

@PublishedApi
internal abstract class PublishedSealedClass {
    protected fun impl() {}
    protected open fun open() {}
    protected abstract fun abstract()
}

sealed class SealedClass {
    protected fun impl() {}
    protected open fun open() {}
    protected abstract fun abstract()
}

enum class EnumClass {
    A {
        override fun abstract() {
        }

        override fun open() {
        }
    };

    protected open fun open() {}
    protected abstract fun abstract()

}
