import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val originalPluginJar: Configuration by configurations.creating

dependencies {
    originalPluginJar(ideaPluginJarDep())
    embedded(ultimateProjectDep(":cidr-native")) { isTransitive = false }
}

val prepareKotlinPluginXml: Task by prepareKotlinPluginXml(originalPluginJar)
val patchFileTemplates: Task by patchFileTemplates(originalPluginJar)
val patchGradleXml: Task by patchGradleXml(originalPluginJar)

// Pack Jar file with patched files (KotlinPlugin.xml, file templates) plus shadowed project classes.
pluginJar(
        originalPluginJar,
        listOf(prepareKotlinPluginXml, patchFileTemplates, patchGradleXml)
)
