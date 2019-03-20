import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.pill.PillExtension.Variant

plugins { java }

description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

val packedJars by configurations.creating
dependencies {
    packedJars(project(":kotlin-scripting-impl")) { isTransitive = false }
    packedJars(project(":kotlin-scripting-compiler")) { isTransitive = false }
    packedJars(project(":kotlin-scripting-common")) { isTransitive = false }
    packedJars(project(":kotlin-scripting-jvm")) { isTransitive = false }
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
