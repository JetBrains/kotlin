import java.util.stream.Stream

fun main(args: Array<String>) {
<caret>  Stream.iterate(0) { i -> i!! + 1 }.skip(Stream.of(1).skip(Stream.empty<Any>().count()).count()).limit(1).forEach { x -> }
}
