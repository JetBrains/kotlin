fun foo(p: String.(Int) -> Unit){}

fun bar(p: String.(Int) -> Unit) {
    foo()<caret>
}

// EXIST: "{...}"
// EXIST: "{ i -> ... }"
// EXIST: "{ i: Int -> ... }"
// ABSENT: p
