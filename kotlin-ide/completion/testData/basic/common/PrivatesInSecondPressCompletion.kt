import java.io.File

class MyFile : File("file") {
    private val privateField = 0
}

fun foo(f: MyFile) {
    f.<caret>
}

// INVOCATION_COUNT: 2
// EXIST: privateField
// ABSENT: prefixLength