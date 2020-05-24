import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins { java }

description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

val packedJars by configurations.creating
dependencies {
    packedJars(project(":kotlin-scripting-compiler-unshaded")) { isTransitive = false }
    runtime(project(":kotlin-scripting-compiler-impl"))
    runtime(kotlinStdlib())
}

publish()

noDefaultJar()

runtimeJar(rewriteDepsToShadedCompiler(
    tasks.register<ShadowJar>("shadowJar") {
        from(packedJars)
    }
))

sourcesJar()
javadocJar()
