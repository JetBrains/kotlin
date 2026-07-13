plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    @Suppress("UNCHECKED_CAST")
    CompilerModules.kotlinJpsPluginEmbeddedDependencies
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    CompilerModules.kotlinJpsPluginMavenDependencies
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"]
        .let { it as List<String> }
        .forEach { implementation(it) { isTransitive = false } }

    compileOnly(intellijUtilRt())
    compileOnly(intellijPlatformUtil())
    compileOnly(jpsModel())
    compileOnly(jpsModelImpl())
    compileOnly(jpsModelSerialization())
    compileOnly(intellijJDom())
    testCompileOnly(intellijJDom())

    testImplementation(project(":compiler:cli-base"))
    testImplementation(jpsModelSerialization())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { projectDefault() }
}

runtimeJar()
