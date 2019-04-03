
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin Daemon (for using with embeddable compiler)"

plugins {
    `java`
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-daemon")) { isTransitive = false }
}

publish()

noDefaultJar()

runtimeJar(rewriteDepsToShadedCompiler(
        task<ShadowJar>("shadowJar")  {
            from(packedJars)
        }
))

sourcesJar()
javadocJar()
