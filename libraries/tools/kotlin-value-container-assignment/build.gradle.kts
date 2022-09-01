import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    commonApi(project(":kotlin-gradle-plugin-model"))

    commonCompileOnly(project(":compiler"))
    commonCompileOnly(project(":kotlin-value-container-assignment-compiler-plugin"))

    embedded(project(":kotlin-value-container-assignment-compiler-plugin")) { isTransitive = false }

    testImplementation(commonDependency("junit"))
}

gradlePlugin {
    plugins {
        create("valueContainerAssignment") {
            id = "org.jetbrains.kotlin.plugin.value.container.assignment"
            displayName = "Kotlin Value Container Assignment compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.container.assignment.gradle.ValueContainerAssignmentSubplugin"
        }
    }
}