import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins { java }

description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

val packedJars by configurations.creating
dependencies {
    packedJars(project(":kotlin-scripting-compiler")) { isTransitive = false }
    runtimeOnly(project(":kotlin-scripting-compiler-impl-embeddable"))
    runtimeOnly(kotlinStdlib())
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
