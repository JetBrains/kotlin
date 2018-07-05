import java.util.stream.Stream

fun foo() {
    Stream.empty<String>().filter { it.isEmpty() }
}