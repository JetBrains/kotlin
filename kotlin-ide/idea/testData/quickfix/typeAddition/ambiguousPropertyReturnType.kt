// "Specify type explicitly" "true"
package a

interface A {}

interface B {}

class C {
    val <caret>property = object : B, A {}
}