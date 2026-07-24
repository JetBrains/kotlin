import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

sourceSets {
    "bootClasspath" {
        java.srcDirs("bootClasspath")
    }

    main {
        projectDefault()
        compileClasspath += sourceSets["bootClasspath"].output
    }

    test {
        projectDefault()
        compileClasspath += sourceSets["bootClasspath"].output
        runtimeClasspath += sourceSets["bootClasspath"].output
    }
}

val bootClasspathCompileOnly = configurations.getByName("bootClasspathCompileOnly")

dependencies {
    compileOnly(libs.intellij.asm)
    bootClasspathCompileOnly(libs.org.jetbrains.annotations)

    implementation(kotlinStdlib())
    implementation(libs.bytebuddy)
}

val agentJar = tasks.register<ShadowJar>("agentJar") {
    archiveClassifier = "agent"
    from(sourceSets.main.map { it.output })
    configurations = project.configurations.runtimeClasspath.map { listOf(it) }
    manifest {
        attributes["PreMain-Class"] = "org.jetbrains.kotlin.testFramework.TestInstrumentationAgent"
        attributes["Can-Retransform-Classes"] = "true"
    }
}

val bootClasspathJar = tasks.register<Jar>("bootClasspathJar") {
    archiveClassifier = "boot-classpath"
    from(sourceSets["bootClasspath"].output)
}

configurations {
    runtimeElements {
        outgoing {
            artifacts.clear()
            artifact(agentJar)
        }
    }
    consumable("bootClasspath") {
        outgoing {
            artifact(bootClasspathJar)
        }
    }
}
