import java.io.File

class C {
    fun foo(file: File?) {
        file?.delete()
    }
}
