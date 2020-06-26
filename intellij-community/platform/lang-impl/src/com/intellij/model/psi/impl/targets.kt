// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.ReferenceRange
import com.intellij.psi.util.leavesAroundOffset
import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus.Experimental

/**
 * Entry point for obtaining target symbols by [offset] in a [file].
 *
 * @return collection of referenced or declared symbols
 */
@Experimental
fun targetSymbols(file: PsiFile, offset: Int): Collection<Symbol> {
  val (declaredData, referencedData) = declaredReferencedData(file, offset)
  val data = declaredData
             ?: referencedData
             ?: return emptyList()
  return data.targets
}

private val emptyData = DeclaredReferencedData(null, null)

internal fun declaredReferencedData(file: PsiFile, offset: Int): DeclaredReferencedData {
  val allDeclarationsOrReferences = declarationsOrReferences(file, offset)
  if (allDeclarationsOrReferences.isEmpty()) {
    return emptyData
  }

  val withMinimalRanges = chooseByRange(allDeclarationsOrReferences, offset, DeclarationOrReference::rangeWithOffset)

  var declaration: PsiSymbolDeclaration? = null
  val references = ArrayList<PsiSymbolReference>()
  var evaluatorData: TargetData.Evaluator? = null

  for (dr in withMinimalRanges) {
    when (dr) {
      is DeclarationOrReference.Declaration -> {
        if (declaration != null) {
          LOG.error(
            """
            Multiple declarations with the same range are not supported.
            Declaration: $declaration; class: ${declaration.javaClass.name}.
            Another declaration: ${dr.declaration}; class: ${dr.declaration.javaClass.name}.
            """.trimIndent()
          )
        }
        else {
          declaration = dr.declaration
        }
      }
      is DeclarationOrReference.Reference -> {
        references += dr.reference
      }
      is DeclarationOrReference.Evaluator -> {
        LOG.assertTrue(evaluatorData == null)
        evaluatorData = dr.evaluatorData
      }
    }
  }

  return DeclaredReferencedData(
    declaredData = declaration?.let(TargetData::Declared),
    referencedData = evaluatorData ?: references.takeUnless { it.isEmpty() }?.let(TargetData::Referenced)
  )
}

private sealed class DeclarationOrReference {

  abstract val rangeWithOffset: TextRange

  class Declaration(val declaration: PsiSymbolDeclaration) : DeclarationOrReference() {
    override val rangeWithOffset: TextRange get() = declaration.absoluteRange
  }

  class Reference(val reference: PsiSymbolReference) : DeclarationOrReference() {
    override val rangeWithOffset: TextRange get() = reference.absoluteRange
  }

  class Evaluator(private val offset: Int, val evaluatorData: TargetData.Evaluator) : DeclarationOrReference() {
    override val rangeWithOffset: TextRange by lazy(LazyThreadSafetyMode.NONE) {
      when (val origin: PsiOrigin = evaluatorData.origin) {
        is PsiOrigin.Reference -> findRangeWithOffset(origin.reference)
        is PsiOrigin.Leaf -> origin.leaf.textRange
      }
    }

    private fun findRangeWithOffset(reference: PsiReference): TextRange {
      return ReferenceRange.getAbsoluteRanges(reference).find {
        it.containsOffset(offset)
      } ?: error("One of the reference ranges must contain offset at this point")
    }
  }
}

/**
 * @return declarations/references which contain the given [offset] in the [file]
 */
private fun declarationsOrReferences(file: PsiFile, offset: Int): List<DeclarationOrReference> {
  val result = SmartList<DeclarationOrReference>()

  var foundNamedElement: PsiElement? = null

  val allDeclarations = file.allDeclarationsAround(offset)
  if (allDeclarations.isEmpty()) {
    namedElement(file, offset)?.let { (namedElement, leaf) ->
      foundNamedElement = namedElement
      val declaration: PsiSymbolDeclaration = PsiElement2Declaration.createFromDeclaredPsiElement(namedElement, leaf)
      result += DeclarationOrReference.Declaration(declaration)
    }
  }
  else {
    allDeclarations.mapTo(result, DeclarationOrReference::Declaration)
  }

  val allReferences = file.allReferencesAround(offset)
  if (allReferences.isEmpty()) {
    fromTargetEvaluator(file, offset)?.let { evaluatorData ->
      if (foundNamedElement != null && evaluatorData.targetElements.singleOrNull() === foundNamedElement) {
        return@let // treat self-reference as a declaration
      }
      result += DeclarationOrReference.Evaluator(offset, evaluatorData)
    }
  }
  else {
    allReferences.mapTo(result, DeclarationOrReference::Reference)
  }

  return result
}

private data class NamedElementAndLeaf(val namedElement: PsiElement, val leaf: PsiElement)

private fun namedElement(file: PsiFile, offset: Int): NamedElementAndLeaf? {
  for ((leaf, _) in file.leavesAroundOffset(offset)) {
    val namedElement: PsiElement? = TargetElementUtil.getNamedElement(leaf)
    if (namedElement != null) {
      return NamedElementAndLeaf(namedElement, leaf)
    }
  }
  return null
}

private fun fromTargetEvaluator(file: PsiFile, offset: Int): TargetData.Evaluator? {
  val editor = mockEditor(file) ?: return null
  val flags = TargetElementUtil.getInstance().allAccepted and
    TargetElementUtil.ELEMENT_NAME_ACCEPTED.inv() and
    TargetElementUtil.LOOKUP_ITEM_ACCEPTED.inv()
  val reference = TargetElementUtil.findReference(editor, offset)
  val origin: PsiOrigin = if (reference != null) {
    PsiOrigin.Reference(reference)
  }
  else {
    val leaf = file.findElementAt(TargetElementUtil.adjustOffset(file, editor.document, offset)) ?: return null
    PsiOrigin.Leaf(leaf)
  }
  val targetElement = TargetElementUtil.getInstance().findTargetElement(editor, flags, offset)
  val targetElements: List<PsiElement> = when {
    targetElement != null -> listOf(targetElement)
    reference != null -> TargetElementUtil.getInstance().getTargetCandidates(reference).toList()
    else -> emptyList()
  }
  if (targetElements.isEmpty()) {
    return null
  }
  return TargetData.Evaluator(origin, targetElements)
}
