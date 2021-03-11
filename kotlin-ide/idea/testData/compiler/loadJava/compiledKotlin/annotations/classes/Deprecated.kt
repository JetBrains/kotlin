//ALLOW_AST_ACCESS
package test

@Deprecated("Class") class Class {
    @Deprecated("Nested") class Nested

    @Deprecated("Inner") inner class Inner

    @Deprecated("companion object") companion object
}
