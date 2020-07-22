// ERROR: Cannot inline reference from Java

package one

fun <caret>a() {

}

fun b() {
    a()
}
