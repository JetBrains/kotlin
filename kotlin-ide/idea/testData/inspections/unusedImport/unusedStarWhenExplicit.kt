import java.io.File // used
import java.io.* // unused because of explicit imports
import java.io.InputStream // used
import java.io.OutputStream // unused

import java.util.ArrayList // used
import java.util.* // used because HashMap is used

fun foo(file: File, input: InputStream, arrayList: ArrayList<Int>, hashMap: HashMap<Int, Char>) {
}

// WITH_RUNTIME
// FULL_JDK
