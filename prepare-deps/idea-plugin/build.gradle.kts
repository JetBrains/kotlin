import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import org.jetbrains.kotlin.ultimate.*

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
}

repositories {
    teamcityServer {
        setUrl("https://teamcity.jetbrains.com")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val ideaPlugin by configurations.creating { isVisible = false }

dependencies {
    ideaPlugin(tc("$kotlinVersionRepo:$kotlinVersionFull:kotlin-plugin-$kotlinPluginBuildNumber-$kotlinPluginVersion.zip"))
}

val downloadIdeaPlugin by tasks.creating(Copy::class) {
    lazyFrom { zipTree(ideaPlugin.singleFile) }
    into(file(ideaPluginDir).parent)
}
