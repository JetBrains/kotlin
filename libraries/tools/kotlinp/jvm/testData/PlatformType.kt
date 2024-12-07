import java.io.File

class PlatformType {
    fun nullability() = File(".").absoluteFile

    fun mutability() = File(".").toURI().toURL().openConnection().headerFields
}
