import org.jetbrains.intellij.pluginRepository.PluginRepositoryInstance

buildscript {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/jetbrains/intellij-plugin-service")
    }

    dependencies {
        classpath("org.jetbrains.intellij:plugin-repository-rest-client:0.4.32")
    }
}

task("uploadPlugins") {
    doLast {
        val kotlinPluginId = 6954
        val channel = (project.findProperty("plugins.repository.channel") as String?)
            ?.let { if (it == "_default_") null else it }
        val path = project.findProperty("plugins.path") as String? ?: "."
        val token = project.property("plugins.repository.token") as String

        val repo = PluginRepositoryInstance("https://plugins.jetbrains.com/", token)

        val pluginFiles = File(path)
            .listFiles { _, fileName ->
                fileName.toLowerCase().let {
                    it.startsWith("kotlin-plugin") &&
                            it.endsWith(".zip") &&
                            // don't publish CIDR plugins to IDEA channel
                            !it.contains("clion") &&
                            !it.contains("appcode")
                }
            }

        pluginFiles
            ?.sorted()
            ?.forEach { pluginFile ->
                println("Uploading ${pluginFile.name}")
                repo.uploadPlugin(kotlinPluginId, pluginFile, channel)
            }
    }
}
