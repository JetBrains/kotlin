// Remove this hack when IDEA-285445 (Gradle plugin doesn't handle java-rt from IDEA maven repo the same way as idea_rt.jar) is fixed
// https://github.com/JetBrains/intellij-community/blob/0e2aa4030ee763c9b0c828f0b5119f4cdcc66f35/plugins/gradle/java/resources/org/jetbrains/plugins/gradle/java/addTestListener.groovy#L11
if (isIdeaActive) {
    gradle.taskGraph.whenReady {
        allTasks.filterIsInstance<Test>().forEach { task ->
            task.doFirst {
                task.classpath.files.find { it.name.startsWith("java-rt") }?.let { javaRtJar ->
                    try {
                        val urlClassLoader =
                            Class.forName("org.gradle.launcher.daemon.bootstrap.DaemonMain").classLoader as? java.net.URLClassLoader

                        urlClassLoader?.let {
                            it::class.java.getMethod("addURL", java.net.URL::class.java)
                                .invoke(it, javaRtJar.toURI().toURL())
                        }
                    } catch (e: RuntimeException) {
                        logger.log(LogLevel.WARN, "Failed to load java-rt into Gradle daemon", e)
                    }
                }
            }
        }
    }
}