import kotlinApi.KotlinObject
import kotlinApi.KotlinObject.property1
import kotlinApi.KotlinObject.property2

internal class C {
    fun foo(): Int {
        property1 = 1
        property2 = 2
        return KotlinObject.foo() +
                property1 +
                property2
    }
}