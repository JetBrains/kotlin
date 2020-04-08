import java.io.File

fun foo(file: File) {
    if (file.name == <caret>)
}

// EXIST: null
