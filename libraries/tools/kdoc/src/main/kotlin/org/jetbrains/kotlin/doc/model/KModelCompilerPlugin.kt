package org.jetbrains.kotlin.doc.model

import org.jetbrains.jet.cli.common.CompilerPlugin
import org.jetbrains.jet.cli.common.CompilerPluginContext
import org.jetbrains.kotlin.doc.KDocConfig

/** Base class for any compiler plugin which needs to process a KModel */
abstract class KModelCompilerPlugin: CompilerPlugin {

    public open var config: KDocConfig = KDocConfig()


    public override fun processFiles(context: CompilerPluginContext) {
        val bindingContext = context.getContext()
        val sources = context.getFiles()
        if (bindingContext != null && sources != null) {
            val model = KModel(bindingContext, config)
            model.load(sources)

            processModel(model)
        }
    }

    protected abstract fun processModel(model: KModel): Unit
}