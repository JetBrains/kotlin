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
    commonCompileOnly(project(":kotlin-assignment-compiler-plugin"))

    embedded(project(":kotlin-assignment-compiler-plugin")) { isTransitive = false }

    testImplementation(commonDependency("junit"))
}

gradlePlugin {
    plugins {
        create("assignment") {
            id = "org.jetbrains.kotlin.plugin.assignment"
            displayName = "Kotlin Assignment compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentSubplugin"
        }
    }
}
