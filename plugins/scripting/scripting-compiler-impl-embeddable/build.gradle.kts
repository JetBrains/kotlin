import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins { java }

description = "Kotlin Compiler Infrastructure for Scripting for embeddable compiler"

val packedJars by configurations.creating
dependencies {
    packedJars(project(":kotlin-scripting-compiler-impl")) { isTransitive = false }
    runtimeOnly(project(":kotlin-scripting-common"))
    runtimeOnly(project(":kotlin-scripting-jvm"))
    runtimeOnly(kotlinStdlib())
    runtimeOnly(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
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
