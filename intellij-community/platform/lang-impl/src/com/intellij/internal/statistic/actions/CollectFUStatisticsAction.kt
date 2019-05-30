// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.google.gson.GsonBuilder
import com.intellij.BundleBase
import com.intellij.ide.actions.GotoActionBase
import com.intellij.ide.util.gotoByName.ChooseByNameItem
import com.intellij.ide.util.gotoByName.ChooseByNamePopup
import com.intellij.ide.util.gotoByName.ChooseByNamePopupComponent
import com.intellij.ide.util.gotoByName.ListChooseByNameModel
import com.intellij.internal.statistic.eventLog.LogEventSerializer
import com.intellij.internal.statistic.eventLog.newLogEvent
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.internal.statistic.service.fus.collectors.FUStateUsagesLogger
import com.intellij.internal.statistic.service.fus.collectors.FeatureUsagesCollector
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.containers.ContainerUtil
import java.util.*

class CollectFUStatisticsAction : GotoActionBase() {
  override fun gotoActionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val projectCollectors = ExtensionPointName.create<Any>("com.intellij.statistics.projectUsagesCollector").extensionList
    val applicationCollectors = ExtensionPointName.create<Any>("com.intellij.statistics.applicationUsagesCollector").extensionList

    val items = (projectCollectors + applicationCollectors)
      .filterIsInstance(FeatureUsagesCollector::class.java)
      .map { collector ->
        val groupId = collector.groupId
        val className = StringUtil.nullize(collector.javaClass.simpleName, true)
        Item(collector, groupId, className)
      }

    ContainerUtil.sort(items, Comparator.comparing<Item, String> { it -> it.groupId })
    val model = MyChooseByNameModel(project, items)

    val popup = ChooseByNamePopup.createPopup(project, model, getPsiContext(e))
    popup.setShowListForEmptyPattern(true)

    popup.invoke(object : ChooseByNamePopupComponent.Callback() {
      override fun onClose() {
        if (this@CollectFUStatisticsAction.javaClass == myInAction) myInAction = null
      }

      override fun elementChosen(element: Any) {
        showCollectorUsages(project, element as Item, model.useExtendedPresentation)
      }
    }, ModalityState.current(), false)
  }

  private fun showCollectorUsages(project: Project, item: Item, useExtendedPresentation: Boolean) {
    val collector = item.usagesCollector
    val metrics = when (collector) {
      is ApplicationUsagesCollector -> collector.metrics
      is ProjectUsagesCollector -> collector.getMetrics(project)
      else -> throw IllegalArgumentException("Unsupported collector: $collector")
    }
    val groupData = when (collector) {
      is ApplicationUsagesCollector -> collector.data
      is ProjectUsagesCollector -> collector.getData(project)
      else -> throw IllegalArgumentException("Unsupported collector: $collector")
    }

    val gson = GsonBuilder().setPrettyPrinting().create()
    val result = StringBuilder()

    if (useExtendedPresentation) {
      result.append("[\n")
      for (metric in metrics) {
        val metricData = FUStateUsagesLogger.mergeWithEventData(groupData, metric.data)!!.build()
        val event = newLogEvent("test.session", "build", "bucket", System.currentTimeMillis(), collector.groupId,
                                collector.version.toString(), "recorder.version", "event.id", true)
        for (datum in metricData) {
          event.event.addData(datum.key, datum.value)
        }
        val presentation = LogEventSerializer.toString(event)
        result.append(presentation)
        result.append(",\n")
      }
      result.append("]")
    }
    else {
      result.append("{")
      for (metric in metrics) {
        result.append("\"")
        result.append(metric.eventId)
        result.append("\" : ")
        val presentation = gson.toJsonTree(metric.data.build())
        result.append(presentation)
        result.append(",\n")
      }
      result.append("}")
    }

    val fileType = FileTypeManager.getInstance().getStdFileType("JSON")
    val file = LightVirtualFile(item.groupId, fileType, result.toString())
    FileEditorManager.getInstance(project).openFile(file, true)
  }

  private class Item(val usagesCollector: FeatureUsagesCollector,
                     val groupId: String,
                     val className: String?) : ChooseByNameItem {
    override fun getName(): String = groupId
    override fun getDescription(): String? = className
  }

  private class MyChooseByNameModel(project: Project, items: List<Item>)
    : ListChooseByNameModel<Item>(project, "Enter usage collector group id", "No collectors found", items) {

    var useExtendedPresentation: Boolean = false

    override fun getCheckBoxName(): String? = BundleBase.replaceMnemonicAmpersand("&Extended presentation")
    override fun loadInitialCheckBoxState(): Boolean = false
    override fun saveInitialCheckBoxState(state: Boolean) {
      useExtendedPresentation = state
    }

    override fun useMiddleMatching(): Boolean = true
  }
}