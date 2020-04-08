// WITH_RUNTIME
// IS_APPLICABLE: false
import java.io.File

class MyFile : File("file") {
    override fun getCanonicalFile(): File {
        return super.getCanonicalFile()<caret>
    }
}
