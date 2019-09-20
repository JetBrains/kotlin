// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.configurationStore.LOG
import com.intellij.openapi.options.ExternalizableScheme
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.filterSmart
import com.intellij.util.text.UniqueNameGenerator
import gnu.trove.THashSet
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate

internal class SchemeListManager<T : Any>(private val schemeManager: SchemeManagerImpl<T, *>) {
  private val schemesRef = AtomicReference(ContainerUtil.createLockFreeCopyOnWriteList<T>())

  internal val readOnlyExternalizableSchemes = ContainerUtil.newConcurrentMap<String, T>()

  val schemes: MutableList<T>
    get() = schemesRef.get()

  fun replaceSchemeList(oldList: List<T>, newList: List<T>) {
    if (!schemesRef.compareAndSet(oldList, ContainerUtil.createLockFreeCopyOnWriteList(newList))) {
      throw IllegalStateException("Scheme list was modified")
    }
  }

  fun addScheme(scheme: T, replaceExisting: Boolean) {
    var toReplace = -1
    val schemes = schemes
    val processor = schemeManager.processor
    val schemeToInfo = schemeManager.schemeToInfo
    for ((index, existing) in schemes.withIndex()) {
      if (processor.getSchemeKey(existing) != processor.getSchemeKey(scheme)) {
        continue
      }

      toReplace = index
      if (existing === scheme) {
        // do not just return, below scheme will be removed from filesToDelete list
        break
      }

      if (existing.javaClass != scheme.javaClass) {
        LOG.warn("'${processor.getSchemeKey(scheme)}' ${existing.javaClass.simpleName} replaced with ${scheme.javaClass.simpleName}")
      }

      if (replaceExisting && processor.isExternalizable(existing)) {
        val oldInfo = schemeToInfo.remove(existing)
        if (oldInfo != null && processor.isExternalizable(scheme) && !schemeToInfo.containsKey(scheme)) {
          schemeToInfo.put(scheme, oldInfo)
        }
      }
    }

    when {
      toReplace == -1 -> schemes.add(scheme)
      (replaceExisting || !processor.isExternalizable(scheme)) -> {
        if (schemes.get(toReplace) !== scheme) {
          // avoid "set" (LockFreeCopyOnWriteArrayList calls ARRAY_UPDATER.compareAndSet and so on)
          schemes.set(toReplace, scheme)
        }
      }
      else -> {
        (scheme as ExternalizableScheme).renameScheme(
          UniqueNameGenerator.generateUniqueName(scheme.name, collectExistingNames(schemes)))
        schemes.add(scheme)
      }
    }

    if (processor.isExternalizable(scheme) && schemeManager.filesToDelete.isNotEmpty()) {
      schemeToInfo.get(scheme)?.let {
        schemeManager.filesToDelete.remove(it.fileName)
      }
    }

    schemeManager.processPendingCurrentSchemeName(scheme)
  }

  fun setSchemes(newSchemes: List<T>, newCurrentScheme: T?, removeCondition: Predicate<T>?) {
    if (schemes.isNotEmpty()) {
      if (removeCondition == null) {
        schemes.clear()
      }
      else {
        // we must not use remove or removeAll to avoid "equals" call
        schemesRef.set(ContainerUtil.createConcurrentList(schemes.filterSmart { !removeCondition.test(it) }))
      }
    }

    schemes.addAll(newSchemes)

    val oldCurrentScheme = schemeManager.activeScheme
    schemeManager.retainExternalInfo(isScheduleToDelete = true)

    if (oldCurrentScheme != newCurrentScheme) {
      val newScheme: T?
      if (newCurrentScheme != null) {
        schemeManager.activeScheme = newCurrentScheme
        newScheme = newCurrentScheme
      }
      else if (oldCurrentScheme != null && !schemes.contains(oldCurrentScheme)) {
        newScheme = schemes.firstOrNull()
        schemeManager.activeScheme = newScheme
      }
      else {
        newScheme = null
      }

      if (oldCurrentScheme != newScheme) {
        schemeManager.processor.onCurrentSchemeSwitched(oldCurrentScheme, newScheme, false)
      }
    }
  }

  private fun collectExistingNames(schemes: Collection<T>): Collection<String> {
    val result = THashSet<String>(schemes.size)
    schemes.mapTo(result) { schemeManager.processor.getSchemeKey(it) }
    return result
  }
}

private fun ExternalizableScheme.renameScheme(newName: String) {
  if (newName != name) {
    name = newName
    LOG.assertTrue(newName == name)
  }
}