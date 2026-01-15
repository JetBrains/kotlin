import org.jetbrains.kotlin.build.swc.SwcExtension
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcPlugin

project.plugins.apply(SwcPlugin::class.java)
val swcEnvSpec = project.the<SwcEnvSpec>()

val swcKotlinBuild = extensions.create<SwcExtension>(
    "swcKotlinBuild",
    swcEnvSpec,
)

with(swcKotlinBuild) {
    swcEnvSpec.version.set(swcVersion)
}