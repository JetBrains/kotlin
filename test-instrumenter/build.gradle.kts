import org.gradle.jvm.tasks.Jar

plugins {
    java
    kotlin("jvm")
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all") }
}

sourceSets {
    "main" { projectDefault() }
}

tasks {
    "jar" {
        this as Jar
        manifest {
            attributes["Manifest-Version"] = 1.0
            attributes["PreMain-Class"] = "org.jetbrains.kotlin.testFramework.TestInstrumentationAgent"
        }
    }
}