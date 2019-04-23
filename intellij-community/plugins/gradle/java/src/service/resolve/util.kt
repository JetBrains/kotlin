// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.openapi.util.Key
import com.intellij.patterns.PatternCondition
import com.intellij.psi.*
import com.intellij.psi.scope.ElementClassHint
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.gradle.util.GradleConstants.EXTENSION
import org.jetbrains.plugins.groovy.codeInspection.assignment.GrMethodCallInfo
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyMethodResult
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil
import org.jetbrains.plugins.groovy.lang.resolve.NON_CODE
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil.processAllDeclarations
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.getDelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.imports.importedNameKey
import org.jetbrains.plugins.groovy.lang.resolve.processors.AccessorResolverProcessor
import org.jetbrains.plugins.groovy.lang.resolve.processors.ClassHint
import org.jetbrains.plugins.groovy.lang.resolve.processors.MethodResolverProcessor
import java.util.*

val projectTypeKey: Key<GradleProjectAwareType> = Key.create("gradle.current.project")
val saveProjectType: PatternCondition<GroovyMethodResult> = object : PatternCondition<GroovyMethodResult>("saveProjectContext") {
  override fun accepts(result: GroovyMethodResult, context: ProcessingContext?): Boolean {
    // Given the closure matched some method,
    // we want to determine what we know about this Project.
    // This PatternCondition just saves the info into the ProcessingContext.
    context?.put(projectTypeKey, result.candidate?.receiver as? GradleProjectAwareType)
    return true
  }
}

val DELEGATED_TYPE: Key<Boolean> = Key.create("gradle.delegated.type")

/**
 * @author Vladislav.Soroka
 */
internal fun PsiClass?.isResolvedInGradleScript() = this is GroovyScriptClass && this.containingFile.isGradleScript()

internal fun PsiFile?.isGradleScript() = this?.originalFile?.virtualFile?.extension == EXTENSION

@JvmField
val RESOLVED_CODE: Key<Boolean?> = Key.create("gradle.resolved")

fun processDeclarations(aClass: PsiClass,
                        processor: PsiScopeProcessor,
                        state: ResolveState,
                        place: PsiElement): Boolean {
  val name = processor.getHint(com.intellij.psi.scope.NameHint.KEY)?.getName(state)
  if (name == null || !ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) {
    aClass.processDeclarations(processor, state, null, place)
  }
  else {
    val propCandidate = place.references.singleOrNull()?.canonicalText
    if (propCandidate != null) {
      val closure = PsiTreeUtil.getParentOfType(place, GrClosableBlock::class.java)
      val typeToDelegate = closure?.let { getDelegatesToInfo(it)?.typeToDelegate }
      if (typeToDelegate != null) {
        val fqNameToDelegate = TypesUtil.getQualifiedName(typeToDelegate) ?: return true
        val classToDelegate = JavaPsiFacade.getInstance(place.project).findClass(fqNameToDelegate, place.resolveScope) ?: return true
        if (classToDelegate !== aClass) {
          val parent = place.parent
          if (parent is GrMethodCall) {
            if (canBeMethodOf(propCandidate, parent, typeToDelegate)) return true
          }
        }
      }
    }

    val lValue: Boolean = place is GrReferenceExpression && PsiUtil.isLValue(place)
    if (!lValue) {
      val isSetterCandidate = name.startsWith("set")
      val isGetterCandidate = name.startsWith("get")
      val processedSignatures = HashSet<List<String>>()
      if (isGetterCandidate || !isSetterCandidate) {
        val propertyName = name.removePrefix("get").decapitalize()
        for (method in aClass.findMethodsByName(propertyName, true)) {
          processedSignatures.add(method.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map({ it.canonicalText }))
          place.putUserData(RESOLVED_CODE, true)
          if (!processor.execute(method, state)) return false
        }
        for (method in aClass.findMethodsByName("set" + propertyName.capitalize(), true)) {
          if (PsiType.VOID != method.returnType) continue
          if (processedSignatures.contains(method.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map({ it.canonicalText }))) continue
          processedSignatures.add(method.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map({ it.canonicalText }))
          place.putUserData(RESOLVED_CODE, true)
          // hack to pass name check in org.jetbrains.plugins.groovy.lang.resolve.processors.GroovyResolverProcessor
          val newState = state.put<String>(importedNameKey, name)
          if (!processor.execute(method, newState)) return false
        }
      }
      if (!isGetterCandidate && !isSetterCandidate) {
        for (method in aClass.findMethodsByName("get" + name.capitalize(), true)) {
          if (processedSignatures.contains(method.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map({ it.canonicalText }))) continue
          processedSignatures.add(method.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map({ it.canonicalText }))
          place.putUserData(RESOLVED_CODE, true)
          if (!processor.execute(method, state)) return false
        }
      }
      for (method in aClass.findMethodsByName(name, true)) {
        if (processedSignatures.contains(method.getSignature(PsiSubstitutor.EMPTY).parameterTypes.map({ it.canonicalText }))) continue
        place.putUserData(RESOLVED_CODE, true)
        if (!processor.execute(method, state)) return false
      }
    }
    else {
      for (method in aClass.findMethodsByName(name, true)) {
        place.putUserData(RESOLVED_CODE, true)
        if (!processor.execute(method, state)) return false
      }
    }
  }
  return true
}

fun canBeMethodOf(methodName: String,
                  place: GrMethodCall,
                  type: PsiType): Boolean {
  val methodCallInfo = GrMethodCallInfo(place)
  val invoked = methodCallInfo.invokedExpression ?: return false
  val argumentTypes = methodCallInfo.argumentTypes

  val thisType = TypesUtil.boxPrimitiveType(type, place.manager, place.resolveScope)
  val processor = MethodResolverProcessor(methodName, invoked, false, thisType, argumentTypes, PsiType.EMPTY_ARRAY, false)
  val state = ResolveState.initial().let {
    it.put(ClassHint.RESOLVE_CONTEXT, invoked)
    it.put(NON_CODE, false)
  }
  processAllDeclarations(thisType, processor, state, invoked)
  val hasApplicableMethods = processor.hasApplicableCandidates()
  if (hasApplicableMethods) {
    return true
  }

  //search for getters
  for (getterName in GroovyPropertyUtils.suggestGettersName(methodName)) {
    val getterResolver = AccessorResolverProcessor(getterName, methodName, invoked, true, thisType, PsiType.EMPTY_ARRAY)
    processAllDeclarations(thisType, getterResolver, state, invoked)
    if (getterResolver.hasApplicableCandidates()) {
      return true
    }
  }
  //search for setters
  for (setterName in GroovyPropertyUtils.suggestSettersName(methodName)) {
    val getterResolver = AccessorResolverProcessor(setterName, methodName, invoked, false, thisType, PsiType.EMPTY_ARRAY)
    processAllDeclarations(thisType, getterResolver, state, invoked)
    if (getterResolver.hasApplicableCandidates()) {
      return true
    }
  }

  return false
}
