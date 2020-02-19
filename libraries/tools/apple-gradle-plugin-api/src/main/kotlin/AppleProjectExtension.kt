import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import java.io.Serializable

interface AppleProjectExtension : Serializable {
    var teamID: String?
    val targets: NamedDomainObjectContainer<AppleTarget>
    val sourceSets: NamedDomainObjectContainer<AppleSourceSet>

    @JvmDefault
    fun iosApp(): AppleTarget = iosApp {}

    fun iosApp(configure: AppleTarget.() -> Unit): AppleTarget

    @JvmDefault
    fun iosApp(configureClosure: Closure<Any?>): AppleTarget =
        iosApp { ConfigureUtil.configure(configureClosure, this) }

    @JvmDefault
    fun iosApp(name: String): AppleTarget = iosApp(name) {}

    fun iosApp(name: String, configure: AppleTarget.() -> Unit): AppleTarget

    @JvmDefault
    fun iosApp(name: String, configureClosure: Closure<Any?>): AppleTarget =
        iosApp(name) { ConfigureUtil.configure(configureClosure, this) }
}

@get:JvmName("apple")
val Project.apple: AppleProjectExtension get() = extensions.getByType(AppleProjectExtension::class.java)
fun Project.apple(configure: AppleProjectExtension.() -> Unit): AppleProjectExtension = apple.apply { configure() }
