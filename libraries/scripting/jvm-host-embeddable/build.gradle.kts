import org.gradle.jvm.tasks.Jar

description = "Kotlin Scripting JVM host (for using with embeddable compiler)"

plugins { java }

dependencies {
    embedded(project(":kotlin-scripting-jvm-host-unshaded")) { isTransitive = false }
    runtime(project(":kotlin-script-runtime"))
    runtime(kotlinStdlib())
    runtime(project(":kotlin-scripting-common"))
    runtime(project(":kotlin-scripting-jvm"))
    runtime(project(":kotlin-compiler-embeddable"))
    runtime(project(":kotlin-scripting-compiler"))
}

sourceSets {
    "main" {}
    "test" {}
}

publish()

noDefaultJar()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
