
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Kotlin Scripting JSR-223 support"

plugins { java }

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-scripting-jsr223")) { isTransitive = false }
    runtime(project(":kotlin-script-runtime"))
    runtime(kotlinStdlib())
    runtime(project(":kotlin-scripting-common"))
    runtime(project(":kotlin-scripting-jvm"))
    runtime(project(":kotlin-script-util"))
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
    runtime(project(":kotlin-scripting-compiler-embeddable"))
}

sourceSets {
    "main" {}
    "test" {}
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
