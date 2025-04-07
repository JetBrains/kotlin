pluginManagement {
    apply(from = "../../../repo/scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../../../repo/scripts/kotlin-bootstrap.settings.gradle.kts")

    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}
