package foo.bar

import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder

fun LanguageSettingsBuilder.configureFromBuildSrc() {
    println("Hi from BuildSrc: $this")
}