// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util

import com.intellij.execution.Location
import com.intellij.execution.junit.JUnitUtil
import com.intellij.execution.junit2.PsiMemberParameterizedLocation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPackage
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
object GradleExecutionSettingsUtil {

  private fun createTestFilter(filter: String, hasSuffix: Boolean): String {
    return if (hasSuffix) {
      String.format("--tests %s ", filter)
    }
    else {
      String.format("--tests %s", filter)
    }
  }

  private fun createTestFilterFrom(filter: String, hasSuffix: Boolean): String {
    val escaped = filter.replace('"', '*')
    val wrapped = String.format("\"%s\"", escaped)
    return createTestFilter(wrapped, hasSuffix)
  }

  private fun createLocationName(aClass: String?, method: String?): String {
    if (aClass == null) return ""
    val escapedMethod = method?.replace('.', '*')
    return aClass + if (escapedMethod == null) "" else ".$escapedMethod"
  }

  @JvmStatic
  fun createTestFilterFromMethod(aClass: String?, method: String?, hasSuffix: Boolean): String {
    return createTestFilterFrom(createLocationName(aClass, method), hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFromClass(aClass: String?, hasSuffix: Boolean): String {
    if (aClass == null) return ""
    return createTestFilterFrom(aClass, hasSuffix)
  }

  @JvmStatic
  fun createTestWildcardFilter(hasSuffix: Boolean): String {
    return createTestFilter("*", hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFromPackage(aPackage: String, hasSuffix: Boolean): String {
    if (aPackage.isEmpty()) return createTestWildcardFilter(hasSuffix)
    val packageFilter = String.format("%s.*", aPackage)
    return createTestFilterFrom(packageFilter, hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFrom(psiClass: PsiClass, hasSuffix: Boolean): String {
    return createTestFilterFromClass(psiClass.getRuntimeQualifiedName(), hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFrom(psiClass: PsiClass, methodName: String?, hasSuffix: Boolean): String {
    return createTestFilterFromMethod(psiClass.getRuntimeQualifiedName(), methodName, hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFrom(aClass: PsiClass, psiMethod: PsiMethod, hasSuffix: Boolean): String {
    return createTestFilterFrom(aClass, psiMethod.name, hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFrom(psiPackage: PsiPackage, hasSuffix: Boolean): String {
    return createTestFilterFromPackage(psiPackage.qualifiedName, hasSuffix)
  }

  @JvmStatic
  fun createTestFilterFrom(location: Location<*>?, aClass: PsiClass, method: PsiMethod, hasSuffix: Boolean): String {
    var locationName = createLocationName(aClass.getRuntimeQualifiedName(), method.name)
    if (location is PsiMemberParameterizedLocation) {
      val wrappedParamSetName = location.paramSetName
      if (wrappedParamSetName.isNotEmpty()) {
        val paramSetName = wrappedParamSetName
          .removeSurrounding("[", "]")
        locationName += "[*$paramSetName*]"
      }
    }
    else if (aClass.isParameterized()) {
      locationName += "[*]"
    }
    return createTestFilterFrom(locationName, hasSuffix)
  }

  private fun PsiClass.getRuntimeQualifiedName(): String? {
    val parent = parent
    return when (parent) {
      is PsiClass -> parent.getRuntimeQualifiedName() + "$" + name
      else -> qualifiedName
    }
  }

  private fun PsiClass.isParameterized(): Boolean {
    val annotation = JUnitUtil.getRunWithAnnotation(this)
    return annotation != null && JUnitUtil.isParameterized(annotation)
  }
}
