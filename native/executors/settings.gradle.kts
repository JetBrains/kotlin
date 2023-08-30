pluginManagement {
    apply(from = "../../repo/scripts/cache-redirector.settings.gradle.kts")
    apply(from = "../../repo/scripts/kotlin-bootstrap.settings.gradle.kts")

    includeBuild("../../repo/gradle-settings-conventions")
    includeBuild("../../repo/gradle-build-conventions")

    repositories {
        maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
        gradlePluginPortal()
    }
}
