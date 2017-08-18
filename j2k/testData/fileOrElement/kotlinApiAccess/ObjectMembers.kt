import kotlinApi.KotlinObject

internal class C {
    fun foo(): Int {
        KotlinObject.property1 = 1
        KotlinObject.property2 = 2
        return KotlinObject.foo() +
                KotlinObject.property1 +
                KotlinObject.property2
    }
}
