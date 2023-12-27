package test

class Class {
    private companion object
    //↓ "private" becomes the name of the companion object, regular property becomes public
    private val regularProperty = 42
}
