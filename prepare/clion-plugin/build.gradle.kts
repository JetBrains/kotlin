import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val cidrPlugin by configurations.creating
val clionPlatformDepsJar by configurations.creating
val clionPlatformDepsOtherJars by configurations.creating

// Do not rename, used in pill importer
val projectsToShadow by extra(listOf(ultimatePath(":clion-native")))

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
    clionPlatformDepsJar(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "clionPlatformDepsJar"))
    clionPlatformDepsOtherJars(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "clionPlatformDepsOtherJars"))
}

val preparePluginXml by tasks.creating(Copy::class) {
    dependsOn(ultimatePath(":clion-native:assemble"))

    inputs.property("clionPluginVersion", clionPluginVersionFull)
    outputs.dir("$buildDir/$name")

    from(ultimateProject(":clion-native").mainSourceSetOutput.resourcesDir) { include(pluginXmlPath) }
    into(outputs.files.singleFile)

    applyCidrVersionRestrictions(clionVersion, clionVersionStrict, clionPluginVersionFull)
}

val jar = pluginJar {
    dependsOn(cidrPlugin)
    dependsOn(preparePluginXml)

    lazyFrom { zipTree(cidrPlugin.singleFile).matching { exclude(pluginXmlPath) } }

    for (p in projectsToShadow) {
        dependsOn("$p:classes")
        from(getMainSourceSetOutput(p)) { exclude(pluginXmlPath) }
    }
    
    from(preparePluginXml) { include(pluginXmlPath) }
}

val platformDepsJar = task<Zip>("platformDepsJar") {
    dependsOn(clionPlatformDepsJar)
    archiveName = "kotlinNative-platformDeps-CLion.jar"
    destinationDir = file("$buildDir/$name")
    lazyFrom { zipTree(clionPlatformDepsJar.singleFile).matching { exclude(pluginXmlPath) } }
    patchJavaXmls()
}

task<Copy>("clionPlugin") {
    dependsOn(clionPlatformDepsOtherJars)
    into(clionPluginDir)
    from(jar) { into("lib") }
    from(platformDepsJar) { into("lib") }
    lazyFrom({ zipTree(clionPlatformDepsOtherJars.singleFile).files }) { into("lib") }
    from(ultimateProject(":clion-native").file("templates")) { into("templates") }
}
