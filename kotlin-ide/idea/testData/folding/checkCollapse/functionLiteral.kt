fun some(body: () -> Unit) = body()

fun oneMore() <fold text='{...}' expand='true'>{
    some <fold text='{...}' expand='true'>{
        // this is a comment
        val v1 = "Body"
        val v2 = "of"
        val v3 = "function"
    }</fold>
}</fold>

// Generated from: idea/testData/folding/checkCollapse/block.kt