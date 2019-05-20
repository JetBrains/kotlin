import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
}

val cidrPluginTools: Map<String, Any> by rootProject.extra
val prepareKotlinPluginXml: (Project, Configuration) -> NamedDomainObjectContainerCreatingDelegateProvider<Task> by cidrPluginTools
val ideaPluginJarDep: (Project) -> Any by cidrPluginTools
val pluginJar: (Project, Configuration, List<Task>) -> Jar by cidrPluginTools
val patchFileTemplates: (Project, Configuration) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools
val patchGradleXml: (Project, Configuration) -> PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> by cidrPluginTools

val originalPluginJar: Configuration by configurations.creating

dependencies {
    originalPluginJar(ideaPluginJarDep(project))
    embedded(project(":kotlin-ultimate:ide:cidr-native")) { isTransitive = false }
}

val prepareKotlinPluginXmlTask: Task by prepareKotlinPluginXml(project, originalPluginJar)
val patchFileTemplatesTask: Task by patchFileTemplates(project, originalPluginJar)
val patchGradleXmlTask: Task by patchGradleXml(project, originalPluginJar)

// Pack Jar file with patched files (KotlinPlugin.xml, file templates) plus shadowed project classes.
pluginJar(
        project,
        originalPluginJar,
        listOf(prepareKotlinPluginXmlTask, patchFileTemplatesTask, patchGradleXmlTask)
)
