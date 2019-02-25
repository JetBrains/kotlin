import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

// Do not rename, used in pill importer
val projectsToShadow: List<String> by extra(listOf(ultimatePath(":cidr-native")))

val originalPluginJar: Configuration by configurations.creating

dependencies {
    originalPluginJar(ideaPluginJarDep())
}

val prepareKotlinPluginXml: Task by prepareKotlinPluginXml(originalPluginJar)

// Pack Jar file with patched KotlinPlugin.xml plus shadowed project classes.
pluginJar(originalPluginJar, prepareKotlinPluginXml, projectsToShadow)
