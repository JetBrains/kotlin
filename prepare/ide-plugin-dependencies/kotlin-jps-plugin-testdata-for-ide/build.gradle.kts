idePluginDependency {
    apply<JavaPlugin>()

    publish()

    val jar: Jar by tasks

    jar.apply {
        listOf(
            "jps/jps-plugin/testData",
            "compiler/testData/classpathOrder",
        ).forEach {
            from(rootDir.resolve(it)) {
                into(it)
            }
        }
    }
}
