rootProject.name = "buildSrc"

pluginManagement {
    val dokkaRepository = providers.gradleProperty("dokka_repository").getOrElse("https://redirector.kotlinlang.org/maven/dokka-dev")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = dokkaRepository)
    }
}
dependencyResolutionManagement {
    val dokkaRepository = providers.gradleProperty("dokka_repository").getOrElse("https://redirector.kotlinlang.org/maven/dokka-dev")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = dokkaRepository)
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
