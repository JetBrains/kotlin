// "Change to val" "true"
annotation class Ann(
    val a: Int,
    var<caret> b: Int
)
/* FIR_COMPARISON */