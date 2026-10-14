@file:OptIn(InternalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.build.swc.SwcExtension
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.swc.SwcPlugin

project.plugins.apply(SwcPlugin::class.java)
val swcEnvSpec = project.the<SwcEnvSpec>().apply {
    // SWC distributions come from the settings-level `swcDistributions()` repository,
    // so the setup task must not declare a project-level one.
    downloadBaseUrl.set(null as String?)
}

val swcKotlinBuild = extensions.create<SwcExtension>(
    "swcKotlinBuild",
    swcEnvSpec,
)

with(swcKotlinBuild) {
    swcEnvSpec.version.set(swcVersion)
}
