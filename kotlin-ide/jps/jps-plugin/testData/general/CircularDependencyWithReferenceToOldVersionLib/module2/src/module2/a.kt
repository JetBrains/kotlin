package module2

import module1.*

fun bar() {
    JavaClass.oldJavaMethod()
    JavaClass.newJavaMethod()

    KotlinObject.oldKotlinMethod()
    KotlinObject.newKotlinMethod()
}
