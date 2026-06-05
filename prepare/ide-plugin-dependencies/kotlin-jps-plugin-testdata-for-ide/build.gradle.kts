idePluginDependency {
    apply<JavaPlugin>()

    publish()

    val jar = tasks.getByName<Jar>("jar")

    jar.apply {
        listOf("jps/jps-plugin/testData").forEach {
            from(rootDir.resolve(it)) {
                into(it)
            }
        }
    }
}
