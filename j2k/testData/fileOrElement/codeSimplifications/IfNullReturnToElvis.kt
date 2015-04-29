import java.io.File

class C {
    fun foo(file: File): String {
        val parent = file.getParentFile() ?: return ""
        return parent.getName()
    }
}
