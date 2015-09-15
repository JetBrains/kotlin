import java.io.File

internal class C {
    internal fun foo(file: File?) {
        file?.delete()
    }
}
