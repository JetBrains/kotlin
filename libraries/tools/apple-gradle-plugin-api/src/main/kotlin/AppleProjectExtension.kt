import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import java.io.Serializable

interface AppleProjectExtension : Serializable {
    var teamID: String?
    val targets: NamedDomainObjectContainer<AppleTarget>
    val sourceSets: NamedDomainObjectContainer<AppleSourceSet>

    @JvmDefault
    fun iosApp(): AppleTarget = iosApp {}

    fun iosApp(configure: AppleTarget.() -> Unit): AppleTarget

    @JvmDefault
    fun iosApp(name: String): AppleTarget = iosApp(name) {}

    fun iosApp(name: String, configure: AppleTarget.() -> Unit): AppleTarget
}

@get:JvmName("apple")
val Project.apple: AppleProjectExtension get() = extensions.getByType(AppleProjectExtension::class.java)
fun Project.apple(configure: AppleProjectExtension.() -> Unit) = apple.configure()
