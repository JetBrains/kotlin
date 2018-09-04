package test

internal annotation class MyAnnotation

@MyAnnotation
internal class ClassWithParent: FooBar() {
}
