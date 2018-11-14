
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin Scripting JVM host (for using with embeddable compiler)"

plugins { java }

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-scripting-jvm-host")) { isTransitive = false }
    runtime(project(":kotlin-script-runtime"))
    runtime(project(":kotlin-stdlib"))
    runtime(project(":kotlin-scripting-common"))
    runtime(project(":kotlin-scripting-jvm"))
    runtime(project(":kotlin-script-util"))
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" {}
    "test" {}
}

noDefaultJar()

runtimeJar(rewriteDepsToShadedCompiler(
        task<ShadowJar>("shadowJar")  {
            from(packedJars)
        }
))
sourcesJar()
javadocJar()

publish()
