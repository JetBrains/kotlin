// ERROR: Cannot inline reference from Kotlin to Java

package one

fun <caret>a() {

}

fun b() {
    a()
}
