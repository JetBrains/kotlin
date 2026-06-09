import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.intellij.asm)

    implementation(kotlinStdlib())
    implementation(libs.bytebuddy)
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

val agentJar by task<ShadowJar> {
    from(sourceSets.main.map { it.output })
    configurations = project.configurations.runtimeClasspath.map { listOf(it) }
    manifest {
        attributes["PreMain-Class"] = "org.jetbrains.kotlin.testFramework.TestInstrumentationAgent"
        attributes["Can-Retransform-Classes"] = "true"
    }
}

val bootClasspathJar by task<Jar> {
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
