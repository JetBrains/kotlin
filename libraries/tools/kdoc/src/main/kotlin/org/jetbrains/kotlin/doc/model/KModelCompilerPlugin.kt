package org.jetbrains.kotlin.doc.model

import java.io.File
import org.jetbrains.jet.cli.common.CompilerPlugin
import org.jetbrains.jet.cli.common.CompilerPluginContext
import org.jetbrains.kotlin.doc.KDocArguments

/** Base class for any compiler plugin which needs to process a KModel */
abstract class KModelCompilerPlugin(
        // TODO: fix compiler and make protected
        val arguments: KDocArguments)
    : CompilerPlugin
{
    public override fun processFiles(context: CompilerPluginContext) {
        val bindingContext = context.getContext()
        val sources = context.getFiles()
        val sourceDirs: List<File> = arguments.freeArgs.orEmpty().map { path -> File(path) }
        val model = KModel(bindingContext, arguments.apply(), sourceDirs, sources.requireNoNulls())

        processModel(model)
    }

    protected abstract fun processModel(model: KModel): Unit
}
