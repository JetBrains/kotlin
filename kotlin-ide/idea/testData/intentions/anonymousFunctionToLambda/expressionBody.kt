fun foo3(f: () -> Int) {
    f()
}

fun main(args: String) {
    foo3(<caret>fun () = 1)
}