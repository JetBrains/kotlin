package test

class Test {
    @ExposedCopyVisibility
    internal data class ConstructorToBeRemoved internal constructor(internal val fieldToBeRemoved: Any)
}
