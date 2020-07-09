/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.idea.completion.PackageIndexHelper
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityOwnerByValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.scopes.Import
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtStarImportingScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.StarImport
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirStarImportingScope(
    firScope: FirAbstractStarImportingScope,
    builder: KtSymbolByFirBuilder,
    project: Project,
    override val token: ValidityOwner,
) : KtFirDelegatingScope(builder), KtStarImportingScope, ValidityOwnerByValidityToken {
    override val firScope: FirAbstractStarImportingScope = firScope
    override val isDefaultImportingScope: Boolean = withValidityAssertion { firScope is FirDefaultStarImportingScope }
    private val packageHelper = PackageIndexHelper(project)

    override val imports: List<StarImport> by cached {
        firScope.starImports.map { import ->
            StarImport(
                import.packageFqName,
                import.relativeClassName,
                import.resolvedClassId
            )
        }
    }

    // todo cache?
    @OptIn(ExperimentalStdlibApi::class)
    override fun getCallableNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import: Import ->
            if (import.relativeClassName == null) { // top level callable
                packageHelper.getPackageTopLevelNames(import.packageFqName)
            } else { //member
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getCallableNames().orEmpty()
            }
        }
    }

    override fun getClassLikeSymbolNames(): Set<Name> = withValidityAssertion {
        //TODO
        setOf()
    }

}
