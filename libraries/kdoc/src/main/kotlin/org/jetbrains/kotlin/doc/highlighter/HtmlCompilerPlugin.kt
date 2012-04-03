package org.jetbrains.kotlin.doc.highlighter

import org.jetbrains.jet.compiler.CompilerPlugin
import org.jetbrains.jet.compiler.CompilerPluginContext

/**
*/
class HtmlCompilerPlugin: CompilerPlugin {

   public override fun processFiles(context: CompilerPluginContext?) {
        if (context != null) {
            val bindingContext = context.getContext()
            val files = context.getFiles()
            if (bindingContext != null && files != null) {
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
    }
}