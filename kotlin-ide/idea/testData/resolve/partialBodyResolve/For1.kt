fun foo(p: Any?, c: Collection<String>) {
    for (e in c) {
        print(p!!)
    }

    <caret>xxx
}