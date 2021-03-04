package org.jetbrains.dokka.transformers.sources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext

interface AsyncSourceToDocumentableTranslator : SourceToDocumentableTranslator {
    suspend fun invokeSuspending(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DModule

    override fun invoke(sourceSet: DokkaConfiguration.DokkaSourceSet, context: DokkaContext): DModule =
        runBlocking(Dispatchers.Default) {
            invokeSuspending(sourceSet, context)
        }
}
