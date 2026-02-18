import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin

plugins.withType(NodeJsPlugin::class) {
    extensions.configure(NodeJsEnvSpec::class.java) {
        if (kotlinBuildProperties.isCacheRedirectorEnabled.get()) {
            downloadBaseUrl.set("https://cache-redirector.jetbrains.com/nodejs.org/dist")
        }
    }
}
