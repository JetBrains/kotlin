package test

import java.io.File
import utils.name

fun foo(file: File, thread: Thread) {
    print(file.name)
    print(thread.name)
}