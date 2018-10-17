extra["versions.intellijSdk"] = "191-SNAPSHOT"
extra["versions.androidBuildTools"] = "r23.0.1"
extra["versions.idea.NodeJS"] = "181.3494.12"

val gradleJars = listOf(
    "gradle-api",
    "gradle-tooling-api",
    "gradle-base-services",
    "gradle-wrapper",
    "gradle-core",
    "gradle-base-services-groovy"
)

val androidStudioVersion = if (extra.has("versions.androidStudioRelease"))
    extra["versions.androidStudioRelease"]?.toString()?.replace(".", "")?.substring(0, 2)
else
    null

val intellijVersion = rootProject.extra["versions.intellijSdk"] as String
val intellijVersionDelimiterIndex = intellijVersion.indexOfAny(charArrayOf('.', '-'))
if (intellijVersionDelimiterIndex == -1) {
    error("Invalid IDEA version $intellijVersion")
}

val platformBaseVersion = intellijVersion.substring(0, intellijVersionDelimiterIndex)
val platform = androidStudioVersion?.let { "AS$it" } ?: platformBaseVersion

extra["versions.jar.asm-all"] = "7.0-beta"
extra["versions.jar.guava"] = "25.1-jre"
extra["versions.jar.groovy-all"] = "2.4.15"
extra["versions.jar.lombok-ast"] = "0.2.3"
extra["versions.jar.swingx-core"] = "1.6.2-2"
extra["versions.jar.kxml2"] = "2.3.0"
extra["versions.jar.streamex"] = "0.6.7"
extra["versions.jar.gson"] = "2.8.5"
extra["versions.jar.oro"] = "2.0.8"
extra["versions.jar.picocontainer"] = "1.2"
for (jar in gradleJars) {
    extra["versions.jar.$jar"] = "4.5.1"
}

extra["ignore.jar.snappy-in-java"] = true

if (!extra.has("versions.androidStudioRelease")) {
    extra["ignore.jar.android-base-common"] = true
}
