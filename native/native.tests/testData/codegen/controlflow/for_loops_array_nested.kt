import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    val metaArray = arrayOf(
            arrayOf(1, 2, 3),
            arrayOf("Hello"),
            arrayOf<Any>(),
            arrayOf(1..10)
    )
    for (array in metaArray) {
        inner@for (elem in array) {
            if (elem is IntProgression) {
                for (i in elem) {
                    sb.append(i)
                }
                continue@inner
            } else {
                sb.append(elem)
            }
        }
        sb.appendLine()
    }

    assertEquals("""
            123
            Hello
            
            12345678910

        """.trimIndent(), sb.toString())
    return "OK"
}