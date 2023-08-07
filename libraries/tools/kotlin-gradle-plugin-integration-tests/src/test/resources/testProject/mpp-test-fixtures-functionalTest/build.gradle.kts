plugins {
    kotlin("multiplatform")
    java
    `java-test-fixtures`
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm { withJava() }
    val testCompilation = jvm().compilations.getByName("test")
    testCompilation.associateWith(jvm().compilations.getByName("testFixtures"))

    val functionalTestCompilation = jvm().compilations.create("functionalTest")
    functionalTestCompilation.associateWith(testCompilation)

    sourceSets.getByName("jvmTest").dependencies {
        implementation(kotlin("test-junit"))
    }
}

/* Create test task */
val functionalTest = tasks.register<Test>("functionalTest") {
    val compilation = kotlin.jvm().compilations.getByName("functionalTest")
    testClassesDirs = compilation.output.classesDirs
    classpath = compilation.output.classesDirs +
            project.files({ kotlin.jvm().compilations.getByName("functionalTest").runtimeDependencyFiles })

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
