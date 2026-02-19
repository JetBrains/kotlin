package test

class Test {
    @ExposedCopyVisibility
    internal data class ClassToBeRemoved internal constructor(val a: Any)

    @ExposedCopyVisibility
    data class EverythingExceptTheClassAndPropertyToBeRemoved internal constructor(val a: Any)
}
