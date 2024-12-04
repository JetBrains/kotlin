class ClassA {
    val leakedObject: SealedInterface get() = PrivateObject
}

private object PrivateObject : SealedInterface
