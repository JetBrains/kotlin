import gradle.publishGradlePluginsJavadoc

plugins {
    `java-base`
}

if (kotlinBuildProperties.publishGradlePluginsJavadoc) plugins.apply("org.jetbrains.dokka")

extensions.create<PluginApiReferenceExtension>("pluginApiReference", project)
