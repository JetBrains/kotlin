import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    api(project(":kotlin-gradle-plugin-model"))

    compileOnly(project(":compiler"))
    compileOnly(project(":kotlin-noarg-compiler-plugin"))

    testImplementation(gradleApi())
    testImplementation(commonDependency("junit"))

    embedded(project(":kotlin-noarg-compiler-plugin")) { isTransitive = false }
}

gradlePlugin {
    plugins {
        create("kotlinNoargPlugin") {
            id = "org.jetbrains.kotlin.plugin.noarg"
            displayName = "Kotlin No Arg compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.noarg.gradle.NoArgGradleSubplugin"
        }
        create("kotlinJpaPlugin") {
            id = "org.jetbrains.kotlin.plugin.jpa"
            displayName = "Kotlin JPA compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.noarg.gradle.KotlinJpaSubplugin"
        }
    }
}
