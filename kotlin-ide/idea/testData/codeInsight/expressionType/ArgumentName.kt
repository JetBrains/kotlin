fun foo(a: Int) {}

fun main() {
    foo(<caret>a = 42)
}

// TYPE: a -> <html>Int</html>