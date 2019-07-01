import java.io.File

internal class C {
    fun foo(file: File): String {
        val parent = file.parentFile ?: return ""
        return parent.name
    }
}
