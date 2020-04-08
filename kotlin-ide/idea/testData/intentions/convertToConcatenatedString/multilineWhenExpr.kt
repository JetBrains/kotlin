fun main(args: Array<String>){
    val a = "<caret>${when (1) {
        1 -> 42
        else -> 3
    }}asdfas${when (1) {
        1 -> 42
        else -> 3
    }}"
}
