@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.build.d8.D8Extension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Plugin

if (project == rootProject) {
    error("${project.path} is the root project, apply d8-root-configuration instead of d8-configuration")
}

project.plugins.apply(D8Plugin::class.java)
val d8EnvSpec = project.the<D8EnvSpec>().apply {
    downloadBaseUrl.set(null as String?)
}

val d8KotlinBuild = extensions.create<D8Extension>(
    "d8KotlinBuild",
    project,
    d8EnvSpec,
)

with(d8KotlinBuild) {
    d8EnvSpec.version.set(v8Version)
}
