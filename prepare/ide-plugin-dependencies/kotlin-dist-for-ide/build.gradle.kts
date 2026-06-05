plugins {
    java
}

idePluginDependency {
    publish()

    val jar = tasks.getByName<Jar>("jar")

    jar.apply {
        val compilerProjectName = ":kotlin-compiler"
        val distTaskName = "distKotlinc"

        dependsOn("$compilerProjectName:$distTaskName")
        dependsOn(":kotlin-compiler:dist")

        from {
            val distKotlincTask = project(compilerProjectName).tasks.getByName(distTaskName)
            distKotlincTask.outputs.files
        }
    }

    sourcesJar()
    javadocJar()
}