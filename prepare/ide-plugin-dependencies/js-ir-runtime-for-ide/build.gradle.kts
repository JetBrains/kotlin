plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    java
}

idePluginPublishingLatch {
    publish()

    val jar: Jar by tasks

    jar.apply {
        archiveExtension.set("klib")

        val jsRuntimeProjectName = ":kotlin-stdlib"
        val klibTaskName = "jsJar"

        dependsOn("$jsRuntimeProjectName:$klibTaskName")

        from {
            val klibTask = project(jsRuntimeProjectName).tasks.getByName(klibTaskName)
            zipTree(klibTask.singleOutputFile(layout))
        }
    }
}
