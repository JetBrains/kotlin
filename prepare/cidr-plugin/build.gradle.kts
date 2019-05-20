import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.ultimate.prepareKotlinPluginXml

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extra
val ideaPluginJarDep: (Project) -> Any by cidrPluginTools
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val patchFileTemplates: (Project, Configuration) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools
val patchGradleXml: (Project, Configuration) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools

val originalPluginJar: Configuration by configurations.creating

dependencies {
    originalPluginJar(ideaPluginJarDep(project))
    embedded(project(":kotlin-ultimate:ide:cidr-native")) { isTransitive = false }
}

val prepareKotlinPluginXml: Task by prepareKotlinPluginXml(originalPluginJar)
val patchFileTemplatesTask: Task by patchFileTemplates(project, originalPluginJar)
val patchGradleXmlTask: Task by patchGradleXml(project, originalPluginJar)

// Pack Jar file with patched files (KotlinPlugin.xml, file templates) plus shadowed project classes.
pluginJar(
        project,
        originalPluginJar,
        listOf(prepareKotlinPluginXml, patchFileTemplatesTask, patchGradleXmlTask)
)
