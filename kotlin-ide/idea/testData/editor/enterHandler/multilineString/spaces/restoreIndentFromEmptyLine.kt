fun main(args: Array<String>) {
    println("""
<caret>
        |  sdf""".trimMargin())
}
//-----
fun main(args: Array<String>) {
    println("""

|<caret>
        |  sdf""".trimMargin())
}