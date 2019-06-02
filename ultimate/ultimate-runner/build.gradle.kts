
plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(project(":idea:idea-jvm"))

    runtimeOnly(intellijDep())
    runtimeOnly(files(toolsJar()))
}

val intellijUltimateEnabled : Boolean by rootProject.extra

val ideaUltimatePluginDir: File by rootProject.extra
val ideaUltimateSandboxDir: File by rootProject.extra

if (intellijUltimateEnabled) {
    runIdeTask("runUltimate", ideaUltimatePluginDir, ideaUltimateSandboxDir) {
        dependsOn(":dist", ":ideaPlugin", ":ultimate:ideaUltimatePlugin")
    }
}
