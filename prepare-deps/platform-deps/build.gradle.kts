import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import org.jetbrains.kotlin.ultimate.*

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
}

repositories {
    teamcityServer {
        setUrl("http://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

// Need to separate configurations used for downloading external artifacts (named here as "private*")
// and configurations for referencing downloaded artifacts outside of this project.

val privateCLionPlatformDepsZip by configurations.creating { isVisible = false }
val privateCLionUnscrambledJar by configurations.creating { isVisible = false }

val privateAppCodePlatformDepsZip by configurations.creating { isVisible = false }
val privateAppCodeUnscrambledJar by configurations.creating { isVisible = false }

dependencies {
    privateCLionPlatformDepsZip(tc("$clionVersionRepo:$clionVersion:CL-plugins/kotlinNative-platformDeps-$clionVersion.zip"))
    privateCLionUnscrambledJar(tc("$clionVersionRepo:$clionVersion:unscrambled/clion.jar"))

    privateAppCodePlatformDepsZip(tc("$appcodeVersionRepo:$appcodeVersion:OC-plugins/kotlinNative-platformDeps-$appcodeVersion.zip"))
    privateAppCodeUnscrambledJar(tc("$appcodeVersionRepo:$appcodeVersion:unscrambled/appcode.jar"))
}

addPlatformDepsArtifacts(privateCLionPlatformDepsZip, "clionPlatformDepsJar", "clionPlatformDepsOtherJars")
addPlatformDepsArtifacts(privateAppCodePlatformDepsZip, "appcodePlatformDepsJar", "appcodePlatformDepsOtherJars")

addArtifact(privateCLionUnscrambledJar, "clionUnscrambledJar")
addArtifact(privateAppCodeUnscrambledJar, "appcodeUnscrambledJar")
