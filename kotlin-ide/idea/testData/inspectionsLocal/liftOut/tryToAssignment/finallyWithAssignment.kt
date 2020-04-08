// PROBLEM: none
fun test() {
    var res: String? = null

    <caret>try {
        res = "success"
    } catch (e: Exception) {
        res = "failure"
    } finally {
        res = "finally"
    }
}