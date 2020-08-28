@file:Repository("https://dl.bintray.com/holgerbrandl/mpicbg-scicomp")
//Krangl depends on Klaxon transitively, that's why this repo is needed here
@file:Repository("https://dl.bintray.com/cbeust/maven")
@file:DependsOn("de.mpicbg.scicomp:krangl:0.13", options = arrayOf("scope=compile,runtime"))

import krangl.*

val df = DataFrame.readCSV("libraries/tools/kotlin-main-kts-test/testData/resolve-with-runtime.csv")
df.head().rows.first().let { "${it["name"]} ${it["surname"]}" }
