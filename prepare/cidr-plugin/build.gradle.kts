import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extra
val patchFileTemplates: (Project, Configuration) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools
val patchGradleXml: (Project, Configuration) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools

val originalPluginJar: Configuration by configurations.creating

dependencies {
    originalPluginJar(ideaPluginJarDep())
    embedded(project(":kotlin-ultimate:ide:cidr-native")) { isTransitive = false }
}

val prepareKotlinPluginXml: Task by prepareKotlinPluginXml(originalPluginJar)
val patchFileTemplatesTask: Task by patchFileTemplates(project, originalPluginJar)
val patchGradleXmlTask: Task by patchGradleXml(project, originalPluginJar)

// Pack Jar file with patched files (KotlinPlugin.xml, file templates) plus shadowed project classes.
pluginJar(
        originalPluginJar,
        listOf(prepareKotlinPluginXml, patchFileTemplatesTask, patchGradleXmlTask)
)
