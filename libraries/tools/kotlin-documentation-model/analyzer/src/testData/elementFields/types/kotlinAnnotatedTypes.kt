// MODULE: main
// FILE: example/Types.kt
package example

@MustBeDocumented
@Target(AnnotationTarget.TYPE)
annotation class TypeLabel

class Types {
    val annotated: @TypeLabel String = ""
}
