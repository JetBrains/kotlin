// ERROR: Unresolved reference: stream
// ERROR: Unresolved reference: stream
// ERROR: Unresolved reference: Collectors
import java.util.stream.Collectors

internal class JavaCode {
    fun toJSON(collection: Collection<Int?>): String {
        return "[" + collection.stream().map({ obj: Object -> obj.toString() }).collect(Collectors.joining(", ")).toString() + "]"
    }
}
