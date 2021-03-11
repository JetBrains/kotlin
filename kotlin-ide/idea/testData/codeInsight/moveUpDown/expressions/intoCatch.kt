// MOVE: up
fun test() {
    try {
        run {
        }
    } catch (e: Exception) {
    } catch (e: Throwable) {
    }
    <caret>println()
}