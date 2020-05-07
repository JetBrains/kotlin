plugins {
    java
}

idePluginDependency {
    publish()

    val jar: Jar by tasks

    jar.apply {
        dependsOn(":kotlin-compiler:distKotlinc")
//        from {
//            val distKotlincTask = project(":kotlin-compiler").tasks.getByName("distKotlinc")
//            (distKotlincTask)
//        }

    }

    sourcesJar()
    javadocJar()
}