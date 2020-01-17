// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.configurationStore.LazySchemeProcessor
import com.intellij.configurationStore.SchemeContentChangedHandler
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import gnu.trove.THashSet
import java.util.function.Function

internal interface SchemeChangeEvent {
  fun execute(schemaLoader: Lazy<SchemeLoader<Any, Any>>, schemeManager: SchemeManagerImpl<Any, Any>)
}

internal interface SchemeAddOrUpdateEvent {
  val file: VirtualFile
}

private fun findExternalizableSchemeByFileName(fileName: String, schemeManager: SchemeManagerImpl<Any, Any>): Any? {
  return schemeManager.schemes.firstOrNull {
    fileName == getSchemeFileName(schemeManager, it)
  }
}

internal fun <T : Any> getSchemeFileName(schemeManager: SchemeManagerImpl<T, T>, scheme: T): String {
  return "${schemeManager.getFileName(scheme)}${schemeManager.schemeExtension}"
}

internal fun <T : Any> readSchemeFromFile(file: VirtualFile, schemeLoader: SchemeLoader<T, T>, schemeManager: SchemeManagerImpl<T, T>): T? {
  val fileName = file.name
  if (file.isDirectory || !schemeManager.canRead(fileName)) {
    return null
  }

  return catchAndLog({ file.path }) {
    schemeLoader.loadScheme(fileName, null, file.contentsToByteArray())
  }
}

internal class SchemeChangeApplicator(private val schemeManager: SchemeManagerImpl<Any, Any>) {
  fun reload(events: Collection<SchemeChangeEvent>) {
    val lazySchemeLoader = lazy { schemeManager.createSchemeLoader() }
    doReload(events, lazySchemeLoader)
    if (lazySchemeLoader.isInitialized()) {
      lazySchemeLoader.value.apply()
    }
  }

  private fun doReload(events: Collection<SchemeChangeEvent>, lazySchemaLoader: Lazy<SchemeLoader<Any, Any>>) {
    val oldActiveScheme = schemeManager.activeScheme
    var newActiveScheme: Any? = null

    val processor = schemeManager.processor
    for (event in sortSchemeChangeEvents(events)) {
      event.execute(lazySchemaLoader, schemeManager)

      if (event !is UpdateScheme) {
        continue
      }

      val file = event.file
      if (!file.isValid) {
        continue
      }

      val fileName = file.name
      val changedScheme = findExternalizableSchemeByFileName(fileName, schemeManager)
      if (callSchemeContentChangedIfSupported(changedScheme, fileName, file, schemeManager)) {
        continue
      }

      if (changedScheme != null) {
        lazySchemaLoader.value.removeUpdatedScheme(changedScheme)
        processor.onSchemeDeleted(changedScheme)
      }

      val newScheme = readSchemeFromFile(file, lazySchemaLoader.value, schemeManager)

      fun isNewActiveScheme(): Boolean {
        if (newActiveScheme != null) {
          return false
        }

        if (oldActiveScheme == null) {
          return newScheme != null && schemeManager.currentPendingSchemeName == processor.getSchemeKey(newScheme)
        }
        else {
          // do not set active scheme if currently no active scheme
          // must be equals by reference
          return changedScheme === oldActiveScheme
        }
      }

      if (isNewActiveScheme()) {
        // call onCurrentSchemeSwitched only when all schemes reloaded
        newActiveScheme = newScheme
      }
    }

    if (newActiveScheme != null) {
      schemeManager.activeScheme = newActiveScheme
      processor.onCurrentSchemeSwitched(oldActiveScheme, newActiveScheme, false)
    }
  }
}

// exposed for test only
internal fun sortSchemeChangeEvents(inputEvents: Collection<SchemeChangeEvent>): Collection<SchemeChangeEvent> {
  if (inputEvents.size < 2) {
    return inputEvents
  }

  var isThereSomeRemoveEvent = false


  val existingAddOrUpdate = THashSet<String>()
  val removedFileNames = THashSet<String>()
  val result = ArrayList(inputEvents)
  // first, remove any event before RemoveAllSchemes and remove RemoveScheme event if there is any subsequent add/update
  for (i in (result.size - 1) downTo 0) {
    val event = result.get(i)
    if (event is RemoveAllSchemes) {
      for (j in (i - 1) downTo 0) {
        result.removeAt(j)
      }
      break
    }
    else if (event is SchemeAddOrUpdateEvent) {
      val fileName = event.file.name
      if (removedFileNames.contains(fileName)) {
        result.removeAt(i)
      }
      else {
        existingAddOrUpdate.add(fileName)
      }
    }
    else if (event is RemoveScheme) {
      if (existingAddOrUpdate.contains(event.fileName)) {
        result.removeAt(i)
      }
      else {
        isThereSomeRemoveEvent = true
        removedFileNames.add(event.fileName)
      }
    }
  }

  fun weight(event: SchemeChangeEvent): Int {
    return when (event) {
      is SchemeAddOrUpdateEvent -> 1
      else -> 0
    }
  }

  if (isThereSomeRemoveEvent) {
    // second, move all RemoveScheme to first place, to ensure that SchemeLoader will be not created during processing of RemoveScheme event
    // (because RemoveScheme removes schemes from scheme manager directly)
    result.sortWith(Comparator { o1, o2 ->
      weight(o1) - weight(o2)
    })
  }

  return result
}

private fun callSchemeContentChangedIfSupported(changedScheme: Any?, fileName: String, file: VirtualFile, schemeManager: SchemeManagerImpl<Any, Any>): Boolean {
  if (changedScheme == null || schemeManager.processor !is SchemeContentChangedHandler<*> || schemeManager.processor !is LazySchemeProcessor) {
    return false
  }

  // unrealistic case, but who knows
  val externalInfo = schemeManager.schemeToInfo.get(changedScheme) ?: return false
  catchAndLog({ file.path }) {
    val bytes = file.contentsToByteArray()
    lazyPreloadScheme(bytes, schemeManager.isOldSchemeNaming) { name, parser ->
      val attributeProvider = Function<String, String?> { parser.getAttributeValue(null, it) }
      val schemeName = name
                       ?: schemeManager.processor.getSchemeKey(attributeProvider, FileUtilRt.getNameWithoutExtension(fileName))
                       ?: throw nameIsMissed(bytes)

      val dataHolder = SchemeDataHolderImpl(schemeManager.processor, bytes, externalInfo)
      @Suppress("UNCHECKED_CAST")
      (schemeManager.processor as SchemeContentChangedHandler<Any>).schemeContentChanged(changedScheme, schemeName, dataHolder)
    }
    return true
  }
  return false
}