// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.junit2.PsiMemberParameterizedLocation
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Computable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.MapDataContext
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.junit.runners.Parameterized

abstract class GradleConfigurationProducerTestCase : GradleImportingTestCase() {

  protected fun assertTestFilter(className: String, methodName: String?, testFilter: String) {
    val locationProvider = { c: PsiClass?, m: PsiMethod? -> PsiLocation.fromPsiElement((m ?: c) as PsiElement?) }
    val configurations = getConfigurations(className, methodName, locationProvider)
    assertSize(1, configurations)
    val executionSettings = configurations.first().settings
    assertEquals(testFilter, executionSettings.scriptParameters)
  }

  protected fun assertParameterizedLocationTestFilter(className: String, methodName: String?, paramSetName: String, testFilter: String) {
    val locationProvider = { c: PsiClass?, m: PsiMethod? -> PsiMemberParameterizedLocation(myProject, m!!, c, paramSetName) }
    val configurations = getConfigurations(className, methodName, locationProvider)
    assertSize(1, configurations)
    val executionSettings = configurations.first().settings
    assertEquals(testFilter, executionSettings.scriptParameters)
  }

  private fun getConfigurations(className: String,
                                methodName: String?,
                                locationProvider: (PsiClass?, PsiMethod?) -> Location<*>
  ): List<GradleRunConfiguration> {
    return ApplicationManager.getApplication().runReadAction(Computable {
      val clazz = JavaPsiFacade.getInstance(myProject).findClass(className, GlobalSearchScope.allScope(myProject))
      val method = if (methodName != null) PsiClassImplUtil.findMethodsByName(clazz!!, methodName, false).first() else null
      val dataContext = MapDataContext().apply {
        put(LangDataKeys.PROJECT, myProject)
        put(LangDataKeys.MODULE, ModuleUtilCore.findModuleForPsiElement(clazz!!))
        put(Location.DATA_KEY, locationProvider(clazz, method))
      }

      ConfigurationContext.getFromContext(dataContext).configurationsFromContext
        ?.filter { it.configuration is GradleRunConfiguration }
        ?.map { configurationFromContext -> configurationFromContext.configuration as GradleRunConfiguration }
      ?: emptyList()
    })
  }

  companion object {
    /**
     * It's sufficient to run the test against one gradle version
     */
    @Parameterized.Parameters(name = "with Gradle-{0}")
    @JvmStatic
    fun tests(): Collection<Array<out String>> = arrayListOf(arrayOf(GradleImportingTestCase.BASE_GRADLE_VERSION))
  }
}