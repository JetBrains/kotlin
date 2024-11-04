

gradle.beforeProject {
    if (project.path != ":") {
        if (!project.path.startsWith(":kotlin-ide.")) {
            pluginManager.apply("common-configuration")
        }
        pluginManager.apply("gradle-plugin-common-repositories")
    }
}
