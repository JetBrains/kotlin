plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(platform(project(":jps:jps-bom")))
    implementation(kotlinStdlib())
//    println(kotlinBuildProperties.useBootstrapStdlib)
//    println("================")
    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginEmbeddedDependencies"]
        .let { it as List<String> }
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependencies"]
        .let { it as List<String> }
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

    testImplementation(project(":compiler:cli-common"))
    testImplementation(jpsModelSerialization())
    testImplementation(commonDependency("junit:junit"))
    testImplementation(kotlin("test-junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
