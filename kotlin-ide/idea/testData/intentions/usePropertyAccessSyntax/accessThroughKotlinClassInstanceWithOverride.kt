// WITH_RUNTIME
import java.io.File

class MyFile : File("file") {
    override fun getCanonicalFile(): File {
        return super.getCanonicalFile()
    }
}

fun foo(file: MyFile) {
    file.getCanonicalFile()<caret>
}