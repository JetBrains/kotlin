package cases.interfaces

public interface BaseWithImpl {
    fun foo() = 42
}

public interface DerivedWithImpl : BaseWithImpl {
    override fun foo(): Int {
        return super.foo() + 1
    }
}

public interface DerivedWithoutImpl : BaseWithImpl

