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
val patchFileTemplates: Task by patchFileTemplates(originalPluginJar)
val patchGradleXml: Task by patchGradleXml(originalPluginJar)

// Pack Jar file with patched files (KotlinPlugin.xml, file templates) plus shadowed project classes.
pluginJar(
        originalPluginJar,
        listOf(prepareKotlinPluginXml, patchFileTemplates, patchGradleXml),
        projectsToShadow
)
