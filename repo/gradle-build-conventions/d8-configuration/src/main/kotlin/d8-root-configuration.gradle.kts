@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin

if (project != rootProject) {
    error("${project.path} is not the root project, apply d8-configuration instead of d8-root-configuration")
}
val buildFeatures = serviceOf<BuildFeatures>()
if (!buildFeatures.isolatedProjects.active.get()) {
    plugins.withType<D8Plugin> {
        val d8RootEnvSpec = the<D8EnvSpec>()

        d8RootEnvSpec.version.set(project.kotlinBuildProperties.versionsProperty("v8").get())
    }
}
