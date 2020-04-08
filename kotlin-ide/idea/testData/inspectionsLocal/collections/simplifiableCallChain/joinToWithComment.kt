// WITH_RUNTIME

val sb = StringBuilder()
val x = listOf(1, 2, 3).<caret>map { "$it*$it" }.joinTo(
        // comment1
        buffer = sb,
        // comment2
        prefix = "= ",
        // comment3
        separator = " + "
        // comment4
)