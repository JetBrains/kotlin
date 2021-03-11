import java.util.stream.Stream

fun main(args: Array<String>) {
<caret>  Stream.iterate(0) { i -> i!! + 1 }.skip(Stream.of(1).count()).limit(1).forEach { x -> }
}
