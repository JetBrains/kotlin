import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.build.swc.SwcBridge
import org.jetbrains.kotlin.build.swc.SwcExtension

//import org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec
//import org.jetbrains.kotlin.gradle.targets.js.swc.SwcPlugin

//project.plugins.apply(SwcPlugin::class.java)
//val swcEnvSpec = project.the<SwcEnvSpec>().apply {
//    downloadBaseUrl.set(null as String?)
//}
//
//val swcKotlinBuild = extensions.create<SwcExtension>(
//    "swcKotlinBuild",
//    swcEnvSpec,
//)
//
//with(swcKotlinBuild) {
//    swcEnvSpec.version.set(swcVersion)
//}

// TODO KT-82969: Remove everything below after KGP bootstrap, see org.jetbrains.kotlin.build.swc.SwcBridge doc
//   and uncomment the above code back.

val swcEnvSpec = SwcBridge.applySwcPlugin(project)
SwcBridge.unsetDownloadBaseUrl(swcEnvSpec)
val swcKotlinBuild = extensions.create<SwcExtension>(
    "swcKotlinBuild",
    swcEnvSpec,
)
SwcBridge.setVersion(swcEnvSpec, swcKotlinBuild.swcVersion)
