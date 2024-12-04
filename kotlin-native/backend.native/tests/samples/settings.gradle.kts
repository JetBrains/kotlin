pluginManagement {
    repositories {
        mavenCentral()
    }
}

val hostOs = System.getProperty("os.name")
val isMacos = hostOs == "Mac OS X"
val isLinux = hostOs == "Linux"
val isWindows = hostOs.startsWith("Windows")

/*
 * The following projects are only available for certain platforms.
 * 
 * IMPORTANT: If a new sample doesn't include interop with third-party libraries,
 * add it into the 'buildSamplesWithPlatfromLibs' task in the root build.gradle.
 */
if (isMacos || isLinux || isWindows) {
    include(":csvparser")
    include(":curl")
    include(":echoServer")
    include(":globalState")
    include(":html5Canvas")
    include(":libcurl")
    include(":videoplayer")
    include(":workers")
}

if (isMacos || isLinux) {
    include(":nonBlockingEchoServer")
    include(":tensorflow")
}

if (isMacos) {
    include(":objc")
    include(":opengl")
    include(":uikit")
    include(":watchos")
}

if (isWindows) {
    include(":win32")
}
