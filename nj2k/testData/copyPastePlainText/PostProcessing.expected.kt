package to

import java.io.File
import java.util.ArrayList


class JavaClass {
    internal fun foo(file: File?, target: List<String?>?) {
        val list = ArrayList<String?>()
        if (file != null) {
            list.add(file.name)
        }
        target?.addAll(list)
    }
}
