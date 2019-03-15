
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Annotation Processor for Kotlin (for using with embeddable compiler)"

plugins {
    `java`
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-annotation-processing")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDepsToShadedCompiler(
        task<ShadowJar>("shadowJar")  {
            from(packedJars)
        }
))

sourcesJar()
javadocJar()
