// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.statistics.eventLog

import com.intellij.configurationStore.getStateSpec
import com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEventPrinter
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.xmlb.annotations.Attribute
import org.junit.ClassRule
import org.junit.Test

class FeatureUsageSettingsEventsTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  @Test
  fun `project name to hash`() {
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    assertThat(printer.toHash(projectRule.project)).isNotNull
  }

  @Test
  fun `no project name to hash`() {
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    assertThat(printer.toHash(null)).isNull()
  }

  @Test
  fun `record all default application component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, null)
    assertDefaultState(printer, false, false)
  }

  @Test
  fun `record all default application component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)
    assertDefaultWithoutDefaultRecording(printer, false, false)
  }

  @Test
  fun `record default application component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, null)
    assertDefaultState(printer, false, false)
  }

  @Test
  fun `record default application component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, null)
    assertDefaultWithoutDefaultRecording(printer, false, false)
  }

  @Test
  fun `record all default component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)
    assertDefaultState(printer, true, false)
  }

  @Test
  fun `record all default component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)
    assertDefaultWithoutDefaultRecording(printer, true, false)
  }

  @Test
  fun `record default component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, projectRule.project)
    assertDefaultState(printer, true, false)
  }

  @Test
  fun `record default component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, projectRule.project)
    assertDefaultWithoutDefaultRecording(printer, true, false)
  }

  @Test
  fun `record default multi component with enabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState())
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logDefaultConfigurationState(spec.name, MultiComponentState::class.java, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertDefaultState(printer.getOptionByName("boolOption"), "boolOption", false, withProject, defaultProject)
    assertDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", true, withProject, defaultProject)
  }

  @Test
  fun `record default multi component with disabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState())
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logDefaultConfigurationState(spec.name, MultiComponentState::class.java, projectRule.project)

    assertThat(printer.result).hasSize(1)
    assertDefaultWithoutDefaultRecording(printer, true, false)
  }

  @Test
  fun `record component for default project with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, ProjectManager.getInstance().defaultProject)
    assertDefaultState(printer, false, true)
  }

  @Test
  fun `record component for default project with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, ProjectManager.getInstance().defaultProject)
    assertDefaultWithoutDefaultRecording(printer, false, true)
  }

  @Test
  fun `record not default application component with enabled default recording`() {
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, null)
    assertNotDefaultState(printer, false, false)
  }

  @Test
  fun `record not default application component with disabled default recording`() {
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)

    val withProject = false
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, withProject, defaultProject)
  }

  @Test
  fun `record not default component with enabled default recording`() {
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)
    assertNotDefaultState(printer, true, false)
  }

  @Test
  fun `record not default component with disabled default recording`() {
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, withProject, defaultProject)
  }

  @Test
  fun `record partially not default multi component with enabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, withProject, defaultProject)
    assertDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", true, withProject, defaultProject)
  }

  @Test
  fun `record partially not default multi component with disabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, withProject, defaultProject)
  }

  @Test
  fun `record not default multi component with enabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = false))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", false, withProject, defaultProject)
  }

  @Test
  fun `record not default multi component with disabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = false))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(3)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", false, withProject, defaultProject)
  }

  private fun assertDefaultWithoutDefaultRecording(printer: TestFeatureUsageSettingsEventsPrinter,
                                                   withProject: Boolean,
                                                   defaultProject: Boolean) {
    assertThat(printer.result).hasSize(1)
    assertInvokedRecorded(printer.result[0], withProject, defaultProject)
  }

  private fun assertNotDefaultState(printer: TestFeatureUsageSettingsEventsPrinter, withProject: Boolean, defaultProject: Boolean) {
    assertThat(printer.result).hasSize(1)
    assertNotDefaultState(printer.result[0], "boolOption", true, withProject, defaultProject)
  }

  private fun assertNotDefaultState(event: LoggedComponentStateEvents,
                                    name: String,
                                    value: Any,
                                    withProject: Boolean,
                                    defaultProject: Boolean) {
    assertThat(event.group.id).isEqualTo("settings")
    assertThat(event.group.version > 0).isTrue()
    assertThat(event.id).isEqualTo("MyTestComponent")

    var size = 3
    if (withProject) size++
    if (defaultProject) size++

    assertThat(event.data).hasSize(size)
    assertThat(event.data["name"]).isEqualTo(name)
    assertThat(event.data["value"]).isEqualTo(value)
    assertThat(event.data["default"]).isEqualTo(false)
    if (withProject) {
      assertThat(event.data).containsKey("project")
    }
    if (defaultProject) {
      assertThat(event.data["default_project"]).isEqualTo(true)
    }
  }

  private fun assertDefaultState(printer: TestFeatureUsageSettingsEventsPrinter, withProject: Boolean, defaultProject: Boolean) {
    assertThat(printer.result).hasSize(1)
    assertDefaultState(printer.result[0], "boolOption", false, withProject, defaultProject)
  }

  private fun assertDefaultState(event: LoggedComponentStateEvents,
                                 name: String,
                                 value: Any,
                                 withProject: Boolean,
                                 defaultProject: Boolean) {
    assertThat(event.group.id).isEqualTo("settings")
    assertThat(event.group.version).isGreaterThan(0)
    assertThat(event.id).isEqualTo("MyTestComponent")

    var size = 2
    if (withProject) size++
    if (defaultProject) size++

    assertThat(event.data).hasSize(size)
    assertThat(event.data["name"]).isEqualTo(name)
    assertThat(event.data["value"]).isEqualTo(value)
    if (withProject) {
      assertThat(event.data).containsKey("project")
    }
    if (defaultProject) {
      assertThat(event.data["default_project"]).isEqualTo(true)
    }
  }

  private fun assertInvokedRecorded(event: LoggedComponentStateEvents, withProject: Boolean, defaultProject: Boolean) {
    assertThat(event.group.id).isEqualTo("settings")
    assertThat(event.group.version).isGreaterThan(0)
    assertThat(event.id).isEqualTo("MyTestComponent")

    var size = 1
    if (withProject) size++
    if (defaultProject) size++

    assertThat(event.data).hasSize(size)
    assertThat(event.data["invoked"]).isEqualTo(true)
    if (withProject) {
      assertThat(event.data).containsKey("project")
    }
    if (defaultProject) {
      assertThat(event.data["default_project"]).isEqualTo(true)
    }
  }

  private class TestFeatureUsageSettingsEventsPrinter(recordDefault: Boolean) : FeatureUsageSettingsEventPrinter(recordDefault) {
    val result: MutableList<LoggedComponentStateEvents> = ArrayList()

    override fun logConfig(group: EventLogGroup, eventId: String, data: Map<String, Any>) {
      result.add(LoggedComponentStateEvents(group, eventId, data))
    }

    fun getOptionByName(name: String): LoggedComponentStateEvents {
      for (event in result) {
        if (event.data.containsKey("name") && event.data["name"] == name) {
          return event
        }
      }
      throw RuntimeException("Failed to find event")
    }

    fun getInvokedEvent(): LoggedComponentStateEvents {
      for (event in result) {
        if (event.data.containsKey("invoked") && event.data["invoked"] == true) {
          return event
        }
      }
      throw RuntimeException("Failed to find event")
    }
  }

  private class LoggedComponentStateEvents(val group: EventLogGroup, val id: String, val data: Map<String, Any>)

  @State(name = "MyTestComponent", reportStatistic = true)
  private class TestComponent : PersistentStateComponent<ComponentState> {
    private var state = ComponentState()

    override fun loadState(s: ComponentState) {
      state = s
    }

    override fun getState(): ComponentState? {
      return state
    }
  }

  @Suppress("unused")
  private open class ComponentState(bool: Boolean = false, str: String = "string-option", list: List<Int> = ArrayList()) {
    @Attribute("bool-value")
    val boolOption: Boolean = bool

    @Attribute("str-value")
    val strOption: String = str

    @Attribute("int-values")
    val intOption: List<Int> = list
  }

  @Suppress("unused")
  private class MultiComponentState(bool: Boolean = false,
                                    secondBool: Boolean = true,
                                    str: String = "string-option",
                                    list: List<Int> = ArrayList()) : ComponentState(bool, str, list) {
    @Attribute("second-bool-value")
    val secondBoolOption: Boolean = secondBool
  }
}