@file:OptIn(ExperimentalWasmDsl::class)
// TODO: remove this when KT-75086 is completed
@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.build.d8.D8Extension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.d8.D8EnvSpec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Plugin

project.plugins.apply(D8Plugin::class.java)
rootProject.plugins.apply(D8Plugin::class.java)
val d8EnvSpec = project.the<D8EnvSpec>()
val d8RootEnvSpec = rootProject.the<D8EnvSpec>()

val d8KotlinBuild = extensions.create<D8Extension>(
    "d8KotlinBuild",
    d8EnvSpec,
)

with(d8KotlinBuild) {
    d8RootEnvSpec.version.set(project.v8Version)
    d8EnvSpec.version.set(project.v8Version)
}