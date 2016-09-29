annotation class AllOpen

@AllOpen
annotation class MyComponent

@MyComponent // Double-transitive annotations is not supported
annotation class OtherComponent

class TestWithoutAnnotations_ShouldBeFinal

@AllOpen
class TestAllOpen_ShouldBeOpen

@MyComponent
class TestMyComponent_ShouldBeOpen

@OtherComponent
class TestOtherComponent_ShouldBeFinal

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