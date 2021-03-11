import java.io.File
import kotlin.io.extension as ext

fun foo(file: File): String {
    return file.ex<caret>
}

// COMPLETION_TYPE: SMART
// EXIST: { lookupString: "ext", itemText: "ext", tailText: " for File (kotlin.io.extension)" }
