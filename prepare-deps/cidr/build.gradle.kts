import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import org.jetbrains.kotlin.ultimate.*

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
}

repositories {
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val clion by configurations.creating { isVisible = false }

dependencies {
    clion(tc("$clionVersionRepo:$clionVersion:CLion-$clionVersion.zip"))
}

val downloadCLion by tasks.creating(Copy::class) {
    lazyFrom { zipTree(clion.singleFile) }
    into(clionDir)
}
