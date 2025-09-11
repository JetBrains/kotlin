idePluginDependency {
    apply<JavaPlugin>()

    publish()

    val jar: Jar by tasks

    jar.apply {
        listOf(
            "compiler/testData/asJava/lightClasses",
            "compiler/testData/loadJava/compiledKotlin",
        ).forEach {
            from(rootDir.resolve(it)) {
                into(it)
            }
        }
    }
}
