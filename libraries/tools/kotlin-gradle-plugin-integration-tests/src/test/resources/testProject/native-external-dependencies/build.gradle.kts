plugins {
    id("org.jetbrains.kotlin.multiplatform")
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

val buildExternalDependenciesFile = tasks.register("buildExternalDependenciesFile") {
    val depsBuilder = project.provider {
        val externalDependenciesFile = Class.forName("org.jetbrains.kotlin.gradle.tasks.ExternalDependenciesBuilder")
            .getDeclaredMethod("buildExternalDependenciesFileForTests", Project::class.java).apply { isAccessible = true }
            .invoke(null, project)
            ?.toString().orEmpty()
        externalDependenciesFile
    }

    doLast {
        println("for_test_external_dependencies_file=${depsBuilder.get()}")
    }
}
