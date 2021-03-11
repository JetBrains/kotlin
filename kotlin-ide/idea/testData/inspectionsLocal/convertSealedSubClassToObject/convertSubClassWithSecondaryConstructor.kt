// FIX: Convert sealed sub-class to object
// WITH_RUNTIME

sealed class Sealed

<caret>class SubSealed : Sealed() {
    constructor() {
        println("init")
    }
}