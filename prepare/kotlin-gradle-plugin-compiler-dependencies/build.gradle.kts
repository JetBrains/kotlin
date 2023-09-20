description = "Kotlin Compiler dependencies (used directly in KGP)"

plugins {
    `java`
}

// avoid adding new dependencies here
val compilerModules = listOf(
    ":compiler:cli", // for MessageRenderer, related to MessageCollector usage
    ":compiler:cli-base", // for kotlinx-benchmark
    ":compiler:cli-common", // for compiler arguments setup, for logging via MessageCollector, CompilerSystemProperties, ExitCode
    ":compiler:compiler.version", // for user projects buildscripts
    ":compiler:config", // for kotlinx-benchmark
    ":compiler:config.jvm", // for K2JVMCompilerArguments initialization
    ":compiler:frontend", // for kotlinx-benchmark
    ":compiler:ir.serialization.common", // for kotlinx-benchmark
    ":compiler:ir.tree", // for PartialLinkageMode (K/N)
    ":compiler:util", // for CommonCompilerArguments initialization, K/N
    ":core:compiler.common", // for kotlinx-benchmark
    ":core:compiler.common.jvm", // for FUS statistics parsing all the compiler arguments, otherwise it fails silently
    ":core:compiler.common.native", // for kotlinx-benchmark
    ":core:descriptors", // for kotlinx-benchmark
    ":core:deserialization", // for kotlinx-benchmark
    ":core:deserialization.common", // for kotlinx-benchmark
    ":core:metadata", // for kotlinx-benchmark
    ":core:util.runtime", // for stdlib extensions
    ":js:js.config", // for k/js task
    ":kotlin-build-common", // for incremental compilation setup
)

dependencies {
    for (dependency in compilerModules) {
        embedded(project(dependency)) { isTransitive = false }
    }
    embedded(intellijUtilRt()) { isTransitive = false } // for kapt (PathUtil.getJdkClassesRoots)
    embedded(intellijPlatformUtil()) { isTransitive = false } // for kapt (JavaVersion), KotlinToolRunner (escapeStringCharacters)
    embedded(intellijPlatformUtilBase()) { isTransitive = false } // for kapt (PathUtil.getJdkClassesRoots)
    embedded(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil")) { isTransitive = false } // for kapt (PathUtil.getJdkClassesRoots)
    embedded(jpsModelImpl()) { isTransitive = false } // for kapt (PathUtil.getJdkClassesRoots)
    embedded(protobufLite()) { isTransitive = false } // for kotlinx-benchmark
    embedded(commonDependency("org.jetbrains.intellij.deps:trove4j")) { isTransitive = false } // for k/js task
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar {
    dependsOn(":compiler:ir.tree:generateTree")
}
javadocJar()
