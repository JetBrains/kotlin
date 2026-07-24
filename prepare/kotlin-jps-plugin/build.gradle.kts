plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

dependencies {
    @Suppress("UNCHECKED_CAST")
    CompilerModules.kotlinJpsPluginMavenDependencies
        .forEach { implementation(project(it)) }

    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

@Suppress("UNCHECKED_CAST")
val embeddedDependencies = CompilerModules.kotlinJpsPluginEmbeddedDependencies
publishProjectJars(
    embeddedDependencies + listOf(":jps:jps-plugin", ":jps:jps-common"),
    libraryDependencies = listOf(protobufFull())
)
