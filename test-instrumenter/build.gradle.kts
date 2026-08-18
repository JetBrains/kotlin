@file:Suppress("UnstableApiUsage")

import JdkMajorVersion.JDK_1_8
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.champeau.jmh.JMHTask

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    `java-test-fixtures`
    alias(libs.plugins.jmh)
}

sourceSets {
    "bootClasspath" {
        java.srcDirs("bootClasspath")
    }

    main {
        projectDefault()
        compileClasspath += sourceSets["bootClasspath"].output
    }

    testFixtures {
        projectDefault()
        compileClasspath += sourceSets["bootClasspath"].output
        runtimeClasspath += sourceSets["bootClasspath"].output
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

val bootClasspathCompileOnly = configurations.getByName("bootClasspathCompileOnly")

dependencies {
    compileOnly(libs.intellij.asm)
    bootClasspathCompileOnly(libs.org.jetbrains.annotations)

    api(kotlinStdlib())
    implementation(libs.bytebuddy)

    testFixturesApi(libs.junit.jupiter.api)
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

kotlin {
    // JDK 25 is only for executing tests and benchmarks
    // The instrumentation code itself is compiled with JDK 8
    jvmToolchain(25)
}

testing {
    suites.withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()
    }
}

jmh {
    warmupIterations = 5
    iterations = 10
    fork = 3
    threads = 1
}

tasks {
    compileKotlin {
        configureTaskToolchain(JDK_1_8)
    }

    compileJava {
        configureTaskToolchain(JDK_1_8)
    }

    named<JavaCompile>("compileBootClasspathJava") {
        configureTaskToolchain(JDK_1_8)
    }

    named<JMHTask>("jmh") {
        jmhClasspath.from(sourceSets["bootClasspath"].output)
    }

    named("checkBuild") {
        dependsOn(test)
    }
}
