pluginManagement {
    includeBuild("../../repo/kotlin-build-helpers")
}

plugins {
    id("kotlin-build-helpers")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
    repositories {
        nodeJs()
    }
}
