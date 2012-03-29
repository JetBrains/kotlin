package org.jetbrains.kotlin.doc.highlighter

import org.jetbrains.jet.compiler.CompilerPlugin
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.List
import org.jetbrains.jet.lang.psi.JetFile

/**
 */
class HtmlCompilerPlugin : CompilerPlugin {

    override fun processFiles(bindingContext: BindingContext?, files: List<JetFile?>?) {
        if (files != null && bindingContext != null) {
            for (file in files) {
                if (file != null) {
                    val visitor = HtmlKotlinVisitor()
                    file.accept(visitor)
                }
            }
        }
    }

}