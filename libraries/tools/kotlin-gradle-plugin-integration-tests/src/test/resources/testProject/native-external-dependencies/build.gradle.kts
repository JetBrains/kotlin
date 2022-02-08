plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    <SingleNativeTarget>("host") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
}

afterEvaluate {
    val externalDependenciesFile = Class.forName("org.jetbrains.kotlin.gradle.tasks.ExternalDependenciesBuilder")
        .getDeclaredMethod("buildExternalDependenciesFileForTests", Project::class.java).apply { isAccessible = true }
        .invoke(null, project)
        ?.toString().orEmpty()

    println("for_test_kotlin_native_target=<SingleNativeTarget>")
    println("for_test_external_dependencies_file=$externalDependenciesFile")
}
