// FILE: file1.kt
package foo

private val jsObject: dynamic = js("{str: 'File1Str'}")
private val fileString: String = jsObject.str
fun file1Fun() = fileString

// FILE: file2.kt
package foo

private val jsObject: dynamic = js("{str: 'File2Str'}")
private val fileString: String = jsObject.str
fun file2Fun() = fileString

// FILE: box.kt
package foo

fun box(): String {
    assertEquals(file1Fun(), "File1Str")
    assertEquals(file2Fun(), "File2Str")
    return "OK"
}
