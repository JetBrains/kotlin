plugins {
    id("root-config")
    `java-library`
}

dependencies {
    ProjectModuleLists.kotlinJpsPluginMavenDependencies.forEach { implementation(project(it)) }
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

publishProjectJars(
    ProjectModuleLists.kotlinJpsPluginEmbeddedDependencies + listOf(":jps:jps-plugin", ":jps:jps-common"),
    libraryDependencies = listOf(protobufFull())
)
