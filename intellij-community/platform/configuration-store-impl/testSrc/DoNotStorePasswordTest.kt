// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.serviceContainer.ComponentManagerImpl
import com.intellij.serviceContainer.processAllImplementationClasses
import com.intellij.serviceContainer.processComponentInstancesOfType
import com.intellij.testFramework.ProjectRule
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jdom.Attribute
import org.jdom.Element
import org.junit.ClassRule
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.BiPredicate

class DoNotStorePasswordTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  @Test
  fun printPasswordComponents() {
    val processor = BiPredicate<Class<*>, PluginDescriptor?> { aClass, _ ->
      val stateAnnotation = getStateSpec(aClass)
      if (stateAnnotation == null || stateAnnotation.name.isEmpty()) {
        return@BiPredicate true
      }

      for (i in aClass.genericInterfaces) {
        if (checkType(i)) {
          return@BiPredicate true
        }
      }


      // public static class Project extends WebServersConfigManagerBaseImpl<WebServersConfigManagerBaseImpl.State> {
      // so, we check not only PersistentStateComponent
      checkType(aClass.genericSuperclass)

      true
    }

    val app = ApplicationManager.getApplication() as ComponentManagerImpl
    processAllImplementationClasses(app.picoContainer, processor::test)
    // yes, we don't use default project here to be sure
    processAllImplementationClasses(projectRule.project.picoContainer, processor::test)

    processComponentInstancesOfType(app.picoContainer, PersistentStateComponent::class.java) {
      processor.test(it.javaClass, null)
    }
    processComponentInstancesOfType(projectRule.project.picoContainer, PersistentStateComponent::class.java) {
      processor.test(it.javaClass, null)
    }
  }

  fun check(clazz: Class<*>) {
    @Suppress("DEPRECATION")
    if (clazz.isEnum || clazz === Attribute::class.java || clazz === Element::class.java ||
        clazz === java.lang.String::class.java || clazz === java.lang.Integer::class.java || clazz === java.lang.Boolean::class.java ||
        Map::class.java.isAssignableFrom(clazz) || com.intellij.openapi.util.JDOMExternalizable::class.java.isAssignableFrom(clazz)) {
      return
    }

    for (accessor in XmlSerializerUtil.getAccessors(clazz)) {
      val name = accessor.name
      if (BaseXmlOutputter.doesNameSuggestSensitiveInformation(name)) {
        if (clazz.typeName != "com.intellij.docker.registry.DockerRegistry") {
          throw RuntimeException("${clazz.typeName}.${accessor.name}")
        }
      }
      else if (!accessor.valueClass.isPrimitive) {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        if (Collection::class.java.isAssignableFrom(accessor.valueClass)) {
          val genericType = accessor.genericType
          if (genericType is ParameterizedType) {
            val type = genericType.actualTypeArguments[0]
            if (type is Class<*>) {
              check(type)
            }
          }
        }
        else if (accessor.valueClass != clazz) {
          check(accessor.valueClass)
        }
      }
    }
  }

  private fun checkType(type: Type): Boolean {
    if (type !is ParameterizedType || !PersistentStateComponent::class.java.isAssignableFrom(type.rawType as Class<*>)) {
      return false
    }

    type.actualTypeArguments[0].let {
      if (it is ParameterizedType) {
        check(it.rawType as Class<*>)
      }
      else {
        check(it as Class<*>)
      }
    }
    return true
  }
}