package module1

import module2.*

fun foo() {
    JavaClass.oldJavaMethod()
    JavaClass.newJavaMethod()

    KotlinObject.oldKotlinMethod()
    KotlinObject.newKotlinMethod()
}