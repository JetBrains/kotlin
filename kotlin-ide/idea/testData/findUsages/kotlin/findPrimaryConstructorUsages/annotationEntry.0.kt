// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
annotation class MyAnnotation()

@<caret>MyAnnotation
fun test() {
    MyAnnotation::class.java
}