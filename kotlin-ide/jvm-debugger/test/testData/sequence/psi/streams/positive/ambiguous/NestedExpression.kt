import java.util.stream.Stream

fun main(args: Array<String>) {
<caret>  val c = (Stream.of(1, 2).count() + Stream.of(1).count()) * Stream.of(2).count()
}
