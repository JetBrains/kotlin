plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
}

idePluginPublishingLatch {
    apply<JavaPlugin>()

    publish()

    val jar: Jar by tasks

    jar.apply {
        listOf("jps/jps-plugin/testData").forEach {
            from(rootDir.resolve(it)) {
                into(it)
            }
        }
    }
}
