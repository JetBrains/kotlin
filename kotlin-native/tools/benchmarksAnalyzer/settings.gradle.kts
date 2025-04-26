pluginManagement {
    apply(from = "../../../repo/gradle-settings-conventions/cache-redirector/src/main/kotlin/cache-redirector.settings.gradle.kts")
    apply(from = "../../../repo/gradle-settings-conventions/kotlin-bootstrap/src/main/kotlin/kotlin-bootstrap.settings.gradle.kts")

    repositories {
        maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}
