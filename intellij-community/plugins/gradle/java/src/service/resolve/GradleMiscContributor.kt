// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.patterns.PsiJavaPatterns.psiElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiManager
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_CLOSURE
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DELEGATES_TO_TYPE_KEY
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo

/**
 * @author Vladislav.Soroka
 */
class GradleMiscContributor : GradleMethodContextContributor {
  companion object {
    val useJUnitClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_TASKS_TESTING_TEST, "useJUnit"))
    val testLoggingClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_TASKS_TESTING_TEST, "testLogging"))
    val downloadClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_PROJECT, "download"))
    val domainCollectionWithTypeClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_API_DOMAIN_OBJECT_COLLECTION, "withType"))
    val manifestClosure: GroovyClosurePattern = groovyClosure().inMethod(psiMethod(GRADLE_JVM_TASKS_JAR, "manifest"))
    //    val publicationsClosure = groovyClosure().inMethod(psiMethod("org.gradle.api.publish.PublishingExtension", "publications"))
    const val downloadSpecFqn: String = "de.undercouch.gradle.tasks.download.DownloadSpec"
    const val pluginDependenciesSpecFqn: String = "org.gradle.plugin.use.PluginDependenciesSpec"
  }

  override fun getDelegatesToInfo(closure: GrClosableBlock): DelegatesToInfo? {
    if (useJUnitClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_JUNIT_OPTIONS, closure), Closure.DELEGATE_FIRST)
    }
    if (testLoggingClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_TEST_LOGGING_CONTAINER, closure), Closure.DELEGATE_FIRST)
    }
    if (downloadClosure.accepts(closure)) {
      return DelegatesToInfo(createType(downloadSpecFqn, closure), Closure.DELEGATE_FIRST)
    }
    if (manifestClosure.accepts(closure)) {
      return DelegatesToInfo(createType(GRADLE_API_JAVA_ARCHIVES_MANIFEST, closure), Closure.DELEGATE_FIRST)
    }
    //    if (publicationsClosure.accepts(closure)) {
    //      return DelegatesToInfo(TypesUtil.createType("org.gradle.api.publish.PublicationContainer", closure), Closure.DELEGATE_FIRST)
    //    }

    val parent = closure.parent
    if (domainCollectionWithTypeClosure.accepts(closure)) {
      if (parent is GrMethodCallExpression) {
        val psiElement = parent.argumentList.allArguments.singleOrNull()?.reference?.resolve()
        if (psiElement is PsiClass) {
          return DelegatesToInfo(createType(psiElement.qualifiedName, closure), Closure.DELEGATE_FIRST)
        }
      }
    }

    // resolve closure type to delegate based on return method type, e.g.
    // FlatDirectoryArtifactRepository flatDir(Closure configureClosure)
    if (parent is GrMethodCall) {
      parent.resolveMethod()?.returnType?.let { type ->
        return DelegatesToInfo(type, Closure.DELEGATE_FIRST)
      }
    }
    return null
  }

  override fun process(methodCallInfo: MutableList<String>, processor: PsiScopeProcessor, state: ResolveState, place: PsiElement): Boolean {

    val classHint = processor.getHint(com.intellij.psi.scope.ElementClassHint.KEY)
    val shouldProcessMethods = ResolveUtil.shouldProcessMethods(classHint)
    val groovyPsiManager = GroovyPsiManager.getInstance(place.project)
    val resolveScope = place.resolveScope

    if (shouldProcessMethods && place.parent?.parent is GroovyFile && place.text == "plugins") {
      val pluginsDependenciesClass = JavaPsiFacade.getInstance(place.project).findClass(pluginDependenciesSpecFqn, resolveScope)
                                     ?: return true
      val returnClass = groovyPsiManager.createTypeByFQClassName(pluginDependenciesSpecFqn, resolveScope) ?: return true
      val methodBuilder = GrLightMethodBuilder(place.manager, "plugins").apply {
        containingClass = pluginsDependenciesClass
        returnType = returnClass
      }
      methodBuilder.addAndGetParameter("configuration", GROOVY_LANG_CLOSURE).putUserData(DELEGATES_TO_TYPE_KEY, pluginDependenciesSpecFqn)
      if (!processor.execute(methodBuilder, state)) return false
    }

    if (psiElement().inside(domainCollectionWithTypeClosure).accepts(place)) {

    }
    return true
  }
}
