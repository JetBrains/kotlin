// PROBLEM: "Use of getter method instead of property access syntax"
// WITH_RUNTIME
import java.io.File

fun foo(file: File) {
    file.getAbsolutePath<caret>()
}