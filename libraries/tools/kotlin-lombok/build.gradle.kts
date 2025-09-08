description = "Kotlin lombok compiler plugin"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
    id("project-tests-convention")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

projectTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4)
}

gradlePlugin {
    plugins {
        create("kotlinLombokPlugin") {
            id = "org.jetbrains.kotlin.plugin.lombok"
            displayName = "Kotlin Lombok plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.lombok.gradle.LombokSubplugin"
        }
    }
}
