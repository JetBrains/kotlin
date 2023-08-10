description = "Kotlin Compiler dependencies (used directly in KGP)"

plugins {
    `java`
}

// avoid adding new dependencies here
val compilerModules = listOf(
    ":compiler:cli",
    ":compiler:cli-base", // for kotlinx-benchmark
    ":compiler:cli-common",
    ":compiler:config.jvm",
    ":compiler:config", // for kotlinx-benchmark
    ":compiler:frontend.common",
    ":compiler:frontend", // for kotlinx-benchmark
    ":compiler:ir.tree",
    ":compiler:util",
    ":compiler:compiler.version", // for user projects buildscripts
    ":core:compiler.common", // for kotlinx-benchmark
    ":core:compiler.common.jvm", // for FUS statistics parsing all the compiler arguments, otherwise it fails silently
    ":core:compiler.common.native", // for kotlinx-benchmark
    ":core:descriptors", // for kotlinx-benchmark
    ":core:deserialization.common", // for kotlinx-benchmark
    ":core:deserialization", // for kotlinx-benchmark
    ":core:metadata", // for kotlinx-benchmark
    ":core:util.runtime",
    ":kotlin-build-common",
    ":js:js.config", // for k/js task
)

dependencies {
    for (dependency in compilerModules) {
        embedded(project(dependency)) { isTransitive = false }
    }
    embedded(intellijUtilRt()) { isTransitive = false }
    embedded(intellijPlatformUtil()) { isTransitive = false }
    embedded(intellijPlatformUtilBase()) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil")) { isTransitive = false }
    embedded(jpsModelImpl()) { isTransitive = false }
    embedded(protobufLite()) { isTransitive = false } // for kotlinx-benchmark
    embedded(commonDependency("org.jetbrains.intellij.deps:trove4j")) { isTransitive = false } // for k/js task
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar {
    dependsOn(":compiler:ir.tree:generateTree")
}
javadocJar()
