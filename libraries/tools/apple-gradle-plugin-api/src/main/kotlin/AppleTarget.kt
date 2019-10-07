import org.gradle.api.artifacts.Configuration
import java.io.Serializable

interface AppleTarget : Serializable {
    val name: String
    val configuration: Configuration
    var launchStoryboard: String?
    var mainStoryboard: String?
    var bridgingHeader: String?
}