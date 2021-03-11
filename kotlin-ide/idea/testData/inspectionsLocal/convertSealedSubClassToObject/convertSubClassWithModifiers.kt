// FIX: Convert sealed sub-class to object

sealed class Sealed

private <caret>class SubSealed : Sealed()