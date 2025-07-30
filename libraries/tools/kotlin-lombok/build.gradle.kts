description = "Kotlin lombok compiler plugin"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))
}

compilerTests {
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
