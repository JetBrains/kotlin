import org.jetbrains.kotlin.ultimate.*

plugins {
    kotlin("jvm")
}

val cidrPlugin by configurations.creating
val appcodePlatformDepsJar by configurations.creating
val appcodePlatformDepsOtherJars by configurations.creating

// Do not rename, used in pill importer
val projectsToShadow by extra(listOf(ultimatePath(":appcode-native")))

dependencies {
    cidrPlugin(ultimateProjectDep(":prepare:cidr-plugin"))
    appcodePlatformDepsJar(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "appcodePlatformDepsJar"))
    appcodePlatformDepsOtherJars(ultimateProjectDep(":prepare-deps:platform-deps", configuration = "appcodePlatformDepsOtherJars"))
}

val preparePluginXml by tasks.creating(Copy::class) {
    dependsOn(ultimatePath(":appcode-native:assemble"))

    inputs.property("appcodePluginVersion", appcodePluginVersionFull)
    outputs.dir("$buildDir/$name")

    from(ultimateProject(":appcode-native").mainSourceSetOutput.resourcesDir) { include(pluginXmlPath) }
    into(outputs.files.singleFile)

    applyCidrVersionRestrictions(appcodeVersion, appcodeVersionStrict, appcodePluginVersionFull)
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
    dependsOn(appcodePlatformDepsJar)
    archiveName = "kotlinNative-platformDeps-AppCode.jar"
    destinationDir = file("$buildDir/$name")
    lazyFrom { zipTree(appcodePlatformDepsJar.singleFile).matching { exclude(pluginXmlPath) } }
    patchJavaXmls()
}

task<Copy>("appcodePlugin") {
    dependsOn(appcodePlatformDepsOtherJars)
    into(appcodePluginDir)
    from(jar) { into("lib") }
    from(platformDepsJar) { into("lib") }
    lazyFrom({ zipTree(appcodePlatformDepsOtherJars.singleFile).files }) { into("lib") }
    from(ultimateProject(":appcode-native").file("templates")) { into("templates") }
}
