plugins {
    kotlin("jvm")
}

kotlin.jvmToolchain(11)

dependencies {
    compileOnly(project(":plugins:compose-compiler-plugin:compiler-hosted"))
}

description = "Compiler plugin that enables Compose"

publish {
    artifactId = "kotlin-compose-compiler-plugin-embeddable"
    pom {
        name.set("Compose Compiler")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

runtimeJarWithRelocation {
    configurations = listOf(project.configurations.compileClasspath.get())
    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
}

sourcesJar()
javadocJar()