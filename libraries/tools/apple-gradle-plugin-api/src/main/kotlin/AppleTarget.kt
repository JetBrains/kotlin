import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.AbstractTask
import java.io.Serializable

interface AppleTarget : Serializable {
    val name: String
    val configuration: Configuration
    val sourceSet: AppleSourceSet
    val testSourceSet: AppleSourceSet

    val buildTask: AbstractTask
    val buildTestTask: AbstractTask
    val archiveTask: AbstractTask
    val exportIPATask: AbstractTask

    var launchStoryboard: String?
    var mainStoryboard: String?
    var sceneDelegateClass: String?
    var bridgingHeader: String?
}