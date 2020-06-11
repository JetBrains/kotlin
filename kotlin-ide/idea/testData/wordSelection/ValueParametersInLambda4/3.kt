fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it ->
        it + 1

<selection>        <caret>it + 1
</selection>    }
}