// kotlin-jupyter-lib has runtime dependency on kotlin-jupyter-api, so
// we should be able to import symbols from there
@file:DependsOn("org.jetbrains.kotlinx:kotlin-jupyter-lib:0.8.3.320", options = arrayOf("scope=compile,runtime"))

import org.jetbrains.kotlinx.jupyter.api.HTML

HTML("<p>Paragraph</p>")
