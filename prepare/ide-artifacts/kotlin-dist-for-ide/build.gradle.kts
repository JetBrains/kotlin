plugins {
    java
}

idePluginDependency {
    publish()

    val jar: Jar by tasks

    jar.apply {
        val distKotlincTask = project(":kotlin-compiler").tasks.getByName("distKotlinc")

        dependsOn(distKotlincTask)
        from(distKotlincTask)

    }

    sourcesJar()
    javadocJar()
}