import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation(project(":test-instrumenter:bootclasspath"))
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
            attributes["Can-Retransform-Classes"] = true
//            attributes["Boot-Class-Path"] = true
        }
    }
}