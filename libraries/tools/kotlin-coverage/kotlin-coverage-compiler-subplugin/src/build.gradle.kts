plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

gradlePlugin {
    plugins {
        create("kotlinCoverage") {
            id = "org.jetbrains.kotlin.plugin.coverage"
            displayName = "Kotlin compiler plugin for collecting test coverage"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.coverage.CoverageCompilerSubplugin"
        }
    }
}
