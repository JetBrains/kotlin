// NOTE: This settings file is completely ignored when running composite build `kotlin` + `kotlin-ultimate`.

import java.lang.Boolean.parseBoolean

val cacheRedirectorEnabled: String? by settings

pluginManagement {
    repositories {
        if (parseBoolean(cacheRedirectorEnabled)) {
            maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2")
        }
        gradlePluginPortal()
    }
}

includeSubprojects(
        ":kotlin-ultimate:ide:cidr-native",
        ":kotlin-ultimate:ide:clion-native",
        ":kotlin-ultimate:ide:appcode-native",
        ":kotlin-ultimate:prepare:cidr-plugin",
        ":kotlin-ultimate:prepare:clion-plugin",
        ":kotlin-ultimate:prepare:appcode-plugin"
)

// The root of Kotlin Ultimate has different Gradle paths in standalone and composite build modes.
// - Standalone: ':'
// - Composite: ':kotlin-ultimate'
//
// However, all sub-projects have stable Gradle paths that always start with ':kotlin-ultimate' path prefix
// and don't change when switching between standalone/composite. This makes it easier to work with them.
// Example: ':kotlin-ultimate:ide:clion-native'
//
// There is a redundant sub-project with ':kotlin-ultimate' path in standalone mode.
// This project can't be used as the root of Kotlin Ultimate as there is already root project with path ':'.
// So this redundant project is left empty. Please be careful and avoid running tasks in this project in
// standalone mode, as this will have no effect.
//
// Examples:
// - To clean and build Kotlin Ultimate in standalone mode:
//     ./gradlew :clean :assemble
//     # running ./gradlew :kotlin-ultimate:clean :kotlin-ultimate:assemble will have no effect
//
// - To clean and build Kotlin Ultimate in composite build mode:
//     ./gradlew :kotlin-ultimate:clean :kotlin-ultimate:assemble
//     # running ./gradlew :clean :assemble will cause cleaning and building the whole Kotlin project
//     # including compiler, etc
//
// - To build a specific Kotlin Ultimate module (in any mode):
//     ./gradlew :kotlin-ultimate:ide:clion-native

project(":kotlin-ultimate").projectDir = file("$rootDir/non-existing-dir")

// Include sub-projects with custom filesystem paths.
// All intermediate projects such as ':kotlin-ultimate:ide' are also included with properly configured filesystem paths.
fun includeSubprojects(vararg subprojects: String) {
    subprojects.flatMap { subproject ->
        val dirs = subproject.substringAfter(":kotlin-ultimate:").split(':')
        (1..dirs.size).map { dirs.take(it) }.map {
            it.joinToString(prefix = ":kotlin-ultimate:", separator = ":") to rootDir.resolve(it.joinToString(separator = "/"))
        }
    }.toMap().forEach { (stableGradlePath, fsPath) ->
        include(stableGradlePath)
        project(stableGradlePath).projectDir = file(fsPath)
    }
}
