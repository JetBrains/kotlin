fun run(f: () -> Unit) = 1
fun test() {
<selection>    run {
        // test
        <caret>1
    }
</selection>}