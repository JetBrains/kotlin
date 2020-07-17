
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin Scripting JSR-223 support"

plugins { java }

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-scripting-jsr223-unshaded")) { isTransitive = false }
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-scripting-common"))
    runtimeOnly(project(":kotlin-scripting-jvm"))
    runtimeOnly(project(":kotlin-scripting-jvm-host"))
    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
}

sourceSets {
    "main" {}
    "test" {}
}

publish()

noDefaultJar()

runtimeJar(rewriteDepsToShadedCompiler(
        tasks.register<ShadowJar>("shadowJar")  {
            from(packedJars)
        }
))
sourcesJar()
javadocJar()
