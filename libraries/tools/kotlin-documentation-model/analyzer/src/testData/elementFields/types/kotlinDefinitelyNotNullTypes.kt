// MODULE: main
// FILE: example/Types.kt
package example

class Types {
    fun <R> definitelyNotNull(value: R & Any): R & Any = value
}
