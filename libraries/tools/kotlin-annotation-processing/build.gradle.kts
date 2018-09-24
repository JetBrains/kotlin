import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.jvm.tasks.Jar

description = "Kapt - Annotation processing for Kotlin"

plugins {
    kotlin("jvm")
}

val packedJars by configurations.creating

dependencies {
    compile(project(":kotlin-stdlib"))
    packedJars(project(":kotlin-annotation-processing")) { isTransitive = false }
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
}

projectTest {
    workingDir = projectDir
}

val jar: Jar by tasks
jar.apply {
    classifier = "base"
}

runtimeJar(rewriteDepsToShadedCompiler(
    task<ShadowJar>("shadowJar") {
        from(packedJars)
    }
))

sourcesJar()
javadocJar()

publish()
