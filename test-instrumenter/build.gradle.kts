import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.3"
}

dependencies {
    compileOnly(libs.intellij.asm)

    implementation(kotlinStdlib())
    implementation(libs.bytebuddy)

    testImplementation(libs.junit.jupiter.api)
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

    "jmh" {
        java.srcDirs("jmh")
        compileClasspath += sourceSets["bootClasspath"].output
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

testing.suites.withType<JvmTestSuite>().configureEach {
    useJUnitJupiter()
}

tasks.jmh {
    jmhClasspath.from(sourceSets["bootClasspath"].output)
    profilers = listOf("jfr")
    javaLauncher = getToolchainLauncherFor(JdkMajorVersion.JDK_11_0)
    warmupIterations = 5
    iterations = 5
    fork = 1
    threads = 1

    outputs.upToDateWhen { false }
}
