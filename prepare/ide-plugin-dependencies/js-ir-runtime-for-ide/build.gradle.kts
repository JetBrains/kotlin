plugins {
    java
}

idePluginDependency {
    publish()

    val jar: Jar by tasks

    jar.apply {
        archiveExtension.set("klib")

        val jsRuntimeProjectName = ":kotlin-stdlib-js-ir"
        val klibTaskName = "packFullRuntimeKLib"

        dependsOn("$jsRuntimeProjectName:$klibTaskName")

        from {
            val klibTask = project(jsRuntimeProjectName).tasks.getByName(klibTaskName)
            zipTree(klibTask.singleOutputFile())
        }
    }

    sourcesJar()
    javadocJar()
}