// OUT_OF_CODE_BLOCK: TRUE
// TYPE: 'Int'
// TODO: changes in private properties is still subject to OOCB
class Test {
    val more : Int = 0
    private val test : <caret> = 0
}

// SKIP_ANALYZE_CHECK