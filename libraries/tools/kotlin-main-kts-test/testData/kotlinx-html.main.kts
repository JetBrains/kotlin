#!/usr/bin/env kotlinc -cp dist/kotlinc/lib/kotlin-main-kts.jar -script

@file:Repository("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")

import kotlinx.html.*; import kotlinx.html.stream.*; import kotlinx.html.attributes.*

print(createHTML().html {
    body {
        h1 { +"Hello, World!" }
    }
})

