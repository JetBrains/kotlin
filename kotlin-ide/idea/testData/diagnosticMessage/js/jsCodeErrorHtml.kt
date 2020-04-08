// !DIAGNOSTICS_NUMBER: 4
// !DIAGNOSTICS: JSCODE_ERROR
// !MESSAGE_TYPE: HTML

fun box() {
    val i = "i"
    val n = 10

    js("var      = $n;")

    js("""var a = 10;
    var      = $n;
    var c = 15;""")

    js("""for (var $i =
    )""")

    js("var      = 10;")
}