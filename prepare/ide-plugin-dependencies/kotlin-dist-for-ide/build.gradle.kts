plugins {
    java
}

idePluginDependency {
    publish()

    val jar: Jar by tasks

    jar.apply {
        val compilerProjectName = ":kotlin-compiler"
        val distTaskName = "distKotlinc"

        dependsOn("$compilerProjectName:$distTaskName")

        from {
            val distKotlincTask = project(compilerProjectName).tasks.getByName(distTaskName)
            distKotlincTask.outputs.files
        }
    }

    sourcesJar()
    javadocJar()
}