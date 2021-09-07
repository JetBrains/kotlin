import java.nio.file.Path
import java.util.stream.IntStream
import java.util.function.Consumer

class Foo : Consumer<Path> {
    override fun accept(path: Path) {}
}

fun foo(s: IntStream): List<String> {
    println(s.boxed())
    Any()
    if (s.count() == 0L) throw Exception()
    return object : ArrayList<String>() {}
}
