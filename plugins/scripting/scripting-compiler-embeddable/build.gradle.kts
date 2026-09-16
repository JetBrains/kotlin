description = "Kotlin Scripting Compiler Plugin for embeddable compiler"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

dependencies {
    embedded(project(":kotlin-scripting-compiler")) { isTransitive = false }
    embedded(variantOf(libs.jline) { classifier("jdk8") }) { isTransitive = false }
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-compiler-impl-embeddable"))
    runtimeOnly(kotlinStdlib())
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler()) {
    mergeServiceFiles()
}

sourcesJar()
javadocJar()
