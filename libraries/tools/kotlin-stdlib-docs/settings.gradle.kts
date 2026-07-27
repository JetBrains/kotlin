pluginManagement {
    val dokkaRepository = providers.gradleProperty("dokka_repository").getOrElse("https://redirector.kotlinlang.org/maven/dokka-dev")

    repositories {
        gradlePluginPortal()
        maven(url = dokkaRepository)
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    val dokkaRepository = providers.gradleProperty("dokka_repository").getOrElse("https://redirector.kotlinlang.org/maven/dokka-dev")
    repositories {
        mavenCentral()
        maven(url = dokkaRepository)
    }
}

rootProject.name = "kotlin-stdlib-docs"

include("kotlin_big")
include("kotlin-stdlib")
include("kotlin-reflect")
include("kotlin-test")
include("plugins")
include("plugins:dokka-samples-transformer-plugin")
include("plugins:dokka-version-filter-plugin")
