package org.jetbrains.kotlin.doc.highlighter

import org.jetbrains.jet.cli.common.CompilerPlugin
import org.jetbrains.jet.cli.common.CompilerPluginContext

/**
*/
class HtmlCompilerPlugin: CompilerPlugin {

   public override fun processFiles(context: CompilerPluginContext) {
      val files = context.getFiles()
      for (file in files) {
          if (file != null) {
              val visitor = HtmlKotlinVisitor()
              file.accept(visitor)
          }
      }
    }
}
