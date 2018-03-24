import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.internal.artifacts.publish.ArchivePublishArtifact
import org.gradle.jvm.tasks.Jar

description = "Annotation Processor wrapper for Kotlin"

plugins {
    kotlin("jvm")
}

val packedJars by configurations.creating

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compileOnly(project(":kotlin-annotation-processing"))
    compileOnly(gradleApi())
    testCompile(gradleApi())
    compileOnly("com.android.tools.build:gradle:1.1.0")
    testCompile("com.android.tools.build:gradle:1.1.0")
    testCompile(commonDep("junit:junit"))
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
        task<ShadowJar>("shadowJar")  {
            from(packedJars)
            from(the<JavaPluginConvention>().sourceSets.getByName("main").output)
        }
))
sourcesJar()
javadocJar()

publish()
