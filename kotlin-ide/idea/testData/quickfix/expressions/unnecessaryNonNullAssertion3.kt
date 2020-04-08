// "Remove unnecessary non-null assertion (!!)" "true"
fun test(value : String) : Int {
    return value<caret>!!.length
}
