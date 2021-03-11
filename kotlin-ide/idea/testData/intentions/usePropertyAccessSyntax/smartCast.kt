// WITH_RUNTIME
import java.io.File

fun foo(o: Any) {
    if (o is File) {
        o.getAbsolutePath()<caret>
    }
}