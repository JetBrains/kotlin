// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.chainsSearch

import com.intellij.compiler.backwardRefs.CompilerReferenceServiceEx
import com.intellij.compiler.chainsSearch.context.ChainCompletionContext
import com.intellij.compiler.chainsSearch.context.ChainSearchTarget
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.util.PsiUtil
import org.jetbrains.jps.backwardRefs.CompilerRef
import org.jetbrains.jps.backwardRefs.SignatureData

sealed class RefChainOperation {
  abstract val qualifierRawName: String

  abstract val qualifierDef: CompilerRef.CompilerClassHierarchyElementDef

  abstract val compilerRef: CompilerRef
}

class TypeCast(override val compilerRef: CompilerRef.CompilerClassHierarchyElementDef,
               val castTypeRef: CompilerRef.CompilerClassHierarchyElementDef,
               refService: CompilerReferenceServiceEx): RefChainOperation() {
  override val qualifierRawName: String
    get() = operandName.value
  override val qualifierDef: CompilerRef.CompilerClassHierarchyElementDef
    get() = compilerRef

  private val operandName = lazy(LazyThreadSafetyMode.NONE) {
    refService.getName(compilerRef.name)
  }
}

class MethodCall(override val compilerRef: CompilerRef.JavaCompilerMethodRef,
                 private val signatureData: SignatureData,
                 private val context: ChainCompletionContext): RefChainOperation() {

  private companion object {
    const val CONSTRUCTOR_METHOD_NAME = "<init>"
  }

  override val qualifierRawName: String
    get() = owner.value
  override val qualifierDef: CompilerRef.CompilerClassHierarchyElementDef
    get() = compilerRef.owner

  private val name = lazy(LazyThreadSafetyMode.NONE) {
    context.refService.getName(compilerRef.name)
  }

  private val owner = lazy(LazyThreadSafetyMode.NONE) {
    context.refService.getName(compilerRef.owner.name)
  }

  private val rawReturnType = lazy(LazyThreadSafetyMode.NONE) {
    context.refService.getName(signatureData.rawReturnType)
  }

  val isStatic: Boolean
    get() = signatureData.isStatic

  fun resolve(): Array<PsiMethod> {
    if (CONSTRUCTOR_METHOD_NAME == name.value) {
      return PsiMethod.EMPTY_ARRAY
    }
    val aClass = context.resolvePsiClass(qualifierDef) ?: return PsiMethod.EMPTY_ARRAY
    return aClass.findMethodsByName(name.value, true)
      .filter { it.hasModifierProperty(PsiModifier.STATIC) == isStatic }
      .filter { !it.isDeprecated }
      .filter { context.accessValidator().test(it) }
      .filter {
        val returnType = it.returnType
        when (signatureData.iteratorKind) {
          SignatureData.ARRAY_ONE_DIM -> {
            when (returnType) {
              is PsiArrayType -> {
                val componentType = returnType.componentType
                componentType is PsiClassType && componentType.resolve()?.qualifiedName == rawReturnType.value
              }
              else -> false
            }
          }
          SignatureData.ITERATOR_ONE_DIM -> {
            val iteratorKind = ChainSearchTarget.getIteratorKind(PsiUtil.resolveClassInClassTypeOnly(returnType))
            when {
              iteratorKind != null -> PsiUtil.resolveClassInClassTypeOnly(PsiUtil.substituteTypeParameter(returnType, iteratorKind, 0, false))?.qualifiedName == rawReturnType.value
              else -> false
            }
          }
          SignatureData.ZERO_DIM -> returnType is PsiClassType && returnType.resolve()?.qualifiedName == rawReturnType.value
          else -> throw IllegalStateException("kind is unsupported ${signatureData.iteratorKind}")
        }
      }
      .sortedBy({ it.parameterList.parametersCount })
      .toTypedArray()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as MethodCall

    if (compilerRef.owner != other.compilerRef.owner) return false
    if (compilerRef.name != other.compilerRef.name) return false
    if (signatureData != other.signatureData) return false

    return true
  }

  override fun hashCode(): Int {
    var result = compilerRef.owner.hashCode()
    result = 31 * result + compilerRef.name.hashCode()
    result = 31 * result + signatureData.hashCode()
    return result
  }

  override fun toString(): String {
    return qualifierRawName + (if (isStatic) "." else "#") + name + "(" + compilerRef.parameterCount + ")"
  }
}
