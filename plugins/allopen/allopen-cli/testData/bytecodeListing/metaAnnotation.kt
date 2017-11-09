annotation class AllOpen

@AllOpen
annotation class MyComponent

@MyComponent // Double-transitive annotations is supported
annotation class OtherComponent

@OtherComponent
annotation class AnotherComponent

@java.lang.annotation.Documented
annotation class Documented

class TestWithoutAnnotations_ShouldBeFinal

@Documented
class ClassWithDocumented

@AllOpen
class TestAllOpen_ShouldBeOpen

@MyComponent
class TestMyComponent_ShouldBeOpen

@OtherComponent
class TestOtherComponent_ShouldBeOpen

@AnotherComponent
class TestAnotherComponent_ShouldBeOpen

@MyComponent
abstract class MyComponentBase

class MyComponentImpl_ShouldBeOpen : MyComponentBase() {
    fun method() {}
}

final class MyComponentImpl2_ShouldBeFinal : MyComponentBase() {
    fun method() {}
}

class MyComponentImpl3_ShouldBeOpen : MyComponentBase() {
    final fun method_ShouldBeFinal() {}
}