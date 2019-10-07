import org.gradle.api.file.SourceDirectorySet
import java.io.Serializable

interface AppleSourceSet : Serializable {
    val name: String
    val apple: SourceDirectorySet
}