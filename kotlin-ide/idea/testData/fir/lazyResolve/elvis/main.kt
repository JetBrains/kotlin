package elvis

class WithElvis(val value: String?) {
    fun foo() = <caret>value ?: ""
}


