import okio.FileSystem
import okio.Path.Companion.toPath

expect val HostFileSystem: FileSystem

fun main() {
    HostFileSystem.delete("toto".toPath())
}