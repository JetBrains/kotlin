if (!project.path.startsWith(":kotlin-ide.")) {
    pluginManager.apply("common-configuration")
}
if (!project.path.startsWith(":compiler:build-tools")) {
    pluginManager.apply("com.autonomousapps.dependency-analysis")
}
