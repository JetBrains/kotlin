package test

class Test {
    internal data class ClassToBeRemoved internal constructor(val a: Any)

    data class EverythingExceptTheClassAndPropertyToBeRemoved internal constructor(val a: Any)
}
