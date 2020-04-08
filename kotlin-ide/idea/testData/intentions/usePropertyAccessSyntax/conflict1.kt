// WITH_RUNTIME
// IS_APPLICABLE: false
import java.io.File

fun File.foo(absolutePath: String) {
    getAbsolutePath()<caret>
}