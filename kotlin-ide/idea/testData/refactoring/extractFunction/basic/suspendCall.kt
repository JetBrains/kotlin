fun async(f: suspend () -> Unit) {}

suspend fun await() {}

// SIBLING:
fun main(args: Array<String>) {
    async {
        <selection>await()</selection>
    }
}