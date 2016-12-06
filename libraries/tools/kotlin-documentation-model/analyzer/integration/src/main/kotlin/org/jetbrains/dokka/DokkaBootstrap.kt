package org.jetbrains.dokka

import java.util.function.BiConsumer

interface DokkaBootstrap {

    fun configure(logger: BiConsumer<String, String>,
                  moduleName: String,
                  classpath: List<String>,
                  sources: List<String>,
                  samples: List<String>,
                  includes: List<String>,
                  outputDir: String,
                  format: String,
                  includeNonPublic: Boolean,
                  reportUndocumented: Boolean,
                  skipEmptyPackages: Boolean,
                  skipDeprecated: Boolean,
                  jdkVersion: Int,
                  generateIndexPages: Boolean,
                  sourceLinks: List<String>)

    fun generate()
}