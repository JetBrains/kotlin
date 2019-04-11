import kotlinApi.KotlinObject.foo

internal class C {
    fun bar(): Int {
        return foo()
    }
}
