import java.io.File

internal class C {
    fun foo(file: File?) {
        file?.delete()
    }
}
