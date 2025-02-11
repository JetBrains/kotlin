@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.build.d8.D8Extension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootExtension

project.plugins.apply(D8Plugin::class.java)
val d8EnvSpec = project.the<D8EnvSpec>()
project.rootProject.plugins.apply(D8Plugin::class.java)
val d8Root = project.rootProject.the<D8RootExtension>()

val d8KotlinBuild = extensions.create<D8Extension>(
    "d8KotlinBuild",
    d8EnvSpec,
    d8Root
)

with(d8KotlinBuild) {
    d8EnvSpec.version.set(project.v8Version)

    @Suppress("DEPRECATION", "DEPRECATION_ERROR")
    d8Root.version = project.v8Version
}