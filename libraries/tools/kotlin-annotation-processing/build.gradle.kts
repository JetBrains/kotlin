import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kapt - Annotation processing for Kotlin"

plugins {
    kotlin("jvm")
}

val packedJars by configurations.creating

dependencies {
    api(kotlinStdlib())
    packedJars(project(":kotlin-annotation-processing")) { isTransitive = false }
    runtimeOnly(project(":kotlin-compiler-embeddable"))
}

projectTest(parallel = true) {
    workingDir = projectDir
}

publish()

tasks.named<Jar>("jar").configure {
    archiveClassifier.set("base")
}

runtimeJar(rewriteDepsToShadedCompiler(
    tasks.register<ShadowJar>("shadowJar") {
        from(packedJars)
    }
))

sourcesJar()
javadocJar()
