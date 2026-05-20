plugins {
    java
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
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
}

tasks.jar {
    manifest {
        attributes["PreMain-Class"] = "org.jetbrains.kotlin.testFramework.TestInstrumentationAgent"
        attributes["Can-Retransform-Classes"] = "true"
    }
}

val bootClasspathJar by tasks.registering(Jar::class) {
    archiveClassifier = "boot-classpath"
    from(sourceSets["bootClasspath"].output)
}

val bootClasspath by configurations.registering {
    isCanBeConsumed = true
    outgoing.artifact(bootClasspathJar)
}


