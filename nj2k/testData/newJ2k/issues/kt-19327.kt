import java.util.stream.Collectors

internal class JavaCode {
    fun toJSON(collection: Collection<Int>): String {
        return "[" + collection.stream().map { obj: Int -> obj.toString() }.collect(Collectors.joining(", ")).toString() + "]"
    }
}