import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL for configuring the JVM toolchain (which JDK runs the Kotlin/Java compilers), the emitted
 * bytecode version, and the JDK API surface available to the code, for a project's compile tasks.
 *
 * [jdkApiVersion] is only meaningful when it equals either [targetBytecodeVersion] (stay
 * restricted to the target's API — the default) or [jdkVersion] (opt out of the restriction,
 * exposing the full toolchain JDK's API while still emitting [targetBytecodeVersion] bytecode).
 * Any other value is rejected: neither `javac --release` nor Kotlin's `-Xjdk-release` can express
 * an API-surface restriction that differs from the emitted bytecode version.
 */
abstract class JvmToolchainsExtension @Inject constructor(objects: ObjectFactory) {
    abstract val jdkVersion: Property<JdkMajorVersion>
    abstract val targetBytecodeVersion: Property<JdkMajorVersion>
    abstract val jdkApiVersion: Property<JdkMajorVersion>

    internal val sourceSetConfigurations: NamedDomainObjectContainer<SourceSetToolchainConfiguration> =
        objects.domainObjectContainer(SourceSetToolchainConfiguration::class.java)

    init {
        jdkVersion.convention(DEFAULT_JVM_TOOLCHAIN)
        targetBytecodeVersion.convention(DEFAULT_JVM_TARGET)
        jdkApiVersion.convention(targetBytecodeVersion)
    }

    /**
     * Overrides [jdkVersion]/[targetBytecodeVersion]/[jdkApiVersion] for the compile tasks of a
     * specific source set (e.g. `"test"`, `"jdk25"`). Can be called multiple times, including
     * repeatedly for the same source set to accumulate configuration. Properties left unset here
     * inherit the outer, project-level value.
     */
    fun configureForSourceSet(name: String, action: SourceSetToolchainConfiguration.() -> Unit) {
        val config = sourceSetConfigurations.maybeCreate(name)
        config.jdkVersion.convention(jdkVersion)
        config.targetBytecodeVersion.convention(targetBytecodeVersion)
        config.jdkApiVersion.convention(config.targetBytecodeVersion)
        config.action()
    }
}

abstract class SourceSetToolchainConfiguration(private val sourceSetName: String) : Named {
    abstract val jdkVersion: Property<JdkMajorVersion>
    abstract val targetBytecodeVersion: Property<JdkMajorVersion>
    abstract val jdkApiVersion: Property<JdkMajorVersion>

    override fun getName(): String = sourceSetName
}
