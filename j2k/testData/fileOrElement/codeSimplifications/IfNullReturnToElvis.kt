import java.io.File

class C {
    fun foo(file: File): String {
        val parent = file.parentFile ?: return ""
        return parent.name
    }
}
