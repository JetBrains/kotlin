import kotlinApi.KotlinObject.foo
import kotlinApi.KotlinObject.property1

internal class C {
    fun bar(): Int {
        return foo()
    }
}
