// SHOULD_FAIL_WITH: Class 'Test' already contains function f1(Int)
class Test {
    fun f1(n: Int){}
    companion object{
        fun <caret>f1(n: Int){}
    }
}