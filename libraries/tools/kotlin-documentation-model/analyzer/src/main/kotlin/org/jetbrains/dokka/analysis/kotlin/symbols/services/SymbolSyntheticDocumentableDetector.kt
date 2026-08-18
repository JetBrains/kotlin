/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.analysis.kotlin.internal.SyntheticDocumentableDetector

internal class SymbolSyntheticDocumentableDetector : SyntheticDocumentableDetector {

   /**
    * Currently, it's used only for [org.jetbrains.dokka.base.transformers.documentables.ReportUndocumentedTransformer]
    *
    * For so-called fake-ovveride declarations - we have [InheritedMember] extra.
    * For synthesized declaration - we do not have PSI source.
    *
    * @see org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin.SOURCE_MEMBER_GENERATED
    */
    override fun isSynthetic(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): Boolean {
       @Suppress("UNCHECKED_CAST")
       val extra = (documentable as? WithExtraProperties<Documentable>)?.extra
       val isInherited = extra?.get(InheritedMember)?.inheritedFrom?.get(sourceSet) != null
       // TODO the same for JAVA?
       val isSynthesized = documentable.getPsi(sourceSet) == null
       return isInherited || isSynthesized
    }

    private fun Documentable.getPsi(sourceSet: DokkaConfiguration.DokkaSourceSet): PsiElement? {
        val documentableSource = (this as? WithSources)?.sources?.get(sourceSet) ?: return null
        return when (documentableSource) {
            is PsiDocumentableSource -> documentableSource.psi
            is KtPsiDocumentableSource -> documentableSource.psi
            else -> error("Unknown language sources: ${documentableSource::class}")
        }
    }


}
