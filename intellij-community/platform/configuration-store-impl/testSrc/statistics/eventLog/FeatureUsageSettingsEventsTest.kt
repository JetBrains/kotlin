// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.statistics.eventLog

import com.intellij.configurationStore.getStateSpec
import com.intellij.configurationStore.statistic.eventLog.FeatureUsageSettingsEventPrinter
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ReportValue
import com.intellij.openapi.components.State
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.xmlb.annotations.Attribute
import org.junit.Assert
import org.junit.ClassRule
import org.junit.Test

@Suppress("SameParameterValue")
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
    assertDefaultState(printer, withProject = false, defaultProject = false)
  }

  @Test
  fun `record all default application component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)
    assertDefaultWithoutDefaultRecording(printer, withProject = false, defaultProject = false)
  }

  @Test
  fun `record default application component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, null)
    assertDefaultState(printer, withProject = false, defaultProject = false)
  }

  @Test
  fun `record default application component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, null)
    assertDefaultWithoutDefaultRecording(printer, withProject = false, defaultProject = false)
  }

  @Test
  fun `record all default component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)
    assertDefaultState(printer, withProject = true, defaultProject = false)
  }

  @Test
  fun `record all default component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)
    assertDefaultWithoutDefaultRecording(printer, withProject = true, defaultProject = false)
  }

  @Test
  fun `record default component with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, projectRule.project)
    assertDefaultState(printer, withProject = true, defaultProject = false)
  }

  @Test
  fun `record default component with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logDefaultConfigurationState(spec.name, ComponentState::class.java, projectRule.project)
    assertDefaultWithoutDefaultRecording(printer, withProject = true, defaultProject = false)
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
    assertDefaultState(printer.getOptionByName("boolOption"), "boolOption", false, "bool", withProject, defaultProject)
    assertDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", true, "bool", withProject, defaultProject)
  }

  @Test
  fun `record default multi component with disabled default recording`() {
    val component = TestComponent()
    component.loadState(MultiComponentState())
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logDefaultConfigurationState(spec.name, MultiComponentState::class.java, projectRule.project)

    assertThat(printer.result).hasSize(1)
    assertDefaultWithoutDefaultRecording(printer, withProject = true, defaultProject = false)
  }

  @Test
  fun `record component for default project with enabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, ProjectManager.getInstance().defaultProject)
    assertDefaultState(printer, withProject = false, defaultProject = true)
  }

  @Test
  fun `record component for default project with disabled default recording`() {
    val component = TestComponent()
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, ProjectManager.getInstance().defaultProject)
    assertDefaultWithoutDefaultRecording(printer, withProject = false, defaultProject = true)
  }

  @Test
  fun `record not default application component with enabled default recording`() {
    val withRecordDefault = true
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, null)
    assertNotDefaultState(printer, withRecordDefault, withProject = false, defaultProject = false)
  }

  @Test
  fun `record not default application component with disabled default recording`() {
    val withRecordDefault = false
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)

    val withProject = false
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
  }

  @Test
  fun `record not default component with enabled default recording`() {
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)
    assertNotDefaultState(printer, withRecordDefault = true, withProject = true, defaultProject = false)
  }

  @Test
  fun `record not default component with disabled default recording`() {
    val withRecordDefault = false
    val component = TestComponent()
    component.loadState(ComponentState(bool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
  }

  @Test
  fun `record partially not default multi component with enabled default recording`() {
    val withRecordDefault = true
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
    assertDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", true, "bool", withProject, defaultProject)
  }

  @Test
  fun `record partially not default multi component with disabled default recording`() {
    val withRecordDefault = false
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = true))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
  }

  @Suppress("SameParameterValue")
  @Test
  fun `record not default multi component with enabled default recording`() {
    val withRecordDefault = true
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = false))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(true)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(2)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", false, "bool", withRecordDefault, withProject, defaultProject)
  }

  @Test
  fun `record not default multi component with disabled default recording`() {
    val withRecordDefault = false
    val component = TestComponent()
    component.loadState(MultiComponentState(bool = true, secondBool = false))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, projectRule.project)

    val withProject = true
    val defaultProject = false
    assertThat(printer.result).hasSize(3)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("boolOption"), "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("secondBoolOption"), "secondBoolOption", false, "bool", withRecordDefault, withProject, defaultProject)
  }

  @Test
  fun `record default numerical fields in application component`() {
    val component = TestComponent()
    component.loadState(ComponentStateWithNumerical())
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)

    val withProject = false
    val defaultProject = false
    Assert.assertEquals(1, printer.result.size)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
  }

  @Test
  fun `record not default numerical fields in application component`() {
    val component = TestComponent()
    component.loadState(ComponentStateWithNumerical(intOpt = 10, longOpt = 15, floatOpt = 5.5F, doubleOpt = 3.4))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)

    val withProject = false
    val defaultProject = false
    Assert.assertEquals(5, printer.result.size)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("integerOption"), "integerOption", null, "int", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("longOption"), "longOption", null, "int", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("floatOption"), "floatOption", null, "float", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("doubleOption"), "doubleOption", null, "float", false, withProject, defaultProject)
  }

  @Test
  fun `record not default numerical fields with absolute value in application component`() {
    val component = TestComponent()
    component.loadState(ComponentStateWithNumerical(absIntOpt = 10, absLongOpt = 15, absFloatOpt = 5.5F, absDoubleOpt = 3.4))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)

    val withProject = false
    val defaultProject = false
    Assert.assertEquals(5, printer.result.size)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absIntegerOption"), "absIntegerOption", 10, "int", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absLongOption"), "absLongOption", 15L, "int", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absFloatOption"), "absFloatOption", 5.5f, "float", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absDoubleOption"), "absDoubleOption", 3.4, "float", false, withProject, defaultProject)
  }

  @Test
  fun `record all not default numerical fields with absolute value in application component`() {
    val component = TestComponent()
    component.loadState(ComponentStateWithNumerical(absIntOpt = 10, absLongOpt = 15, absFloatOpt = 5.5F, absDoubleOpt = 3.4))
    val spec = getStateSpec(component)
    val printer = TestFeatureUsageSettingsEventsPrinter(false)
    printer.logConfigurationState(spec.name, component.state, null)

    val withProject = false
    val defaultProject = false
    Assert.assertEquals(5, printer.result.size)
    assertInvokedRecorded(printer.getInvokedEvent(), withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absIntegerOption"), "absIntegerOption", 10, "int", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absLongOption"), "absLongOption", 15L, "int", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absFloatOption"), "absFloatOption", 5.5f, "float", false, withProject, defaultProject)
    assertNotDefaultState(printer.getOptionByName("absDoubleOption"), "absDoubleOption", 3.4, "float", false, withProject, defaultProject)
  }

  private fun assertDefaultWithoutDefaultRecording(printer: TestFeatureUsageSettingsEventsPrinter,
                                                   withProject: Boolean,
                                                   defaultProject: Boolean) {
    assertThat(printer.result).hasSize(1)
    assertInvokedRecorded(printer.result[0], withProject, defaultProject)
  }

  @Suppress("SameParameterValue")
  private fun assertNotDefaultState(printer: TestFeatureUsageSettingsEventsPrinter,withRecordDefault: Boolean, withProject: Boolean, defaultProject: Boolean) {
    assertThat(printer.result).hasSize(1)
    assertNotDefaultState(printer.result[0], "boolOption", true, "bool", withRecordDefault, withProject, defaultProject)
  }

  private fun assertNotDefaultState(event: LoggedComponentStateEvents,
                                    name: String,
                                    value: Any?,
                                    type: String,
                                    withDefaultRecorded: Boolean,
                                    withProject: Boolean,
                                    defaultProject: Boolean) {
    assertThat(event.group.id).isEqualTo("settings")
    assertThat(event.group.version > 0).isTrue()
    assertThat(event.id).isEqualTo(if (withDefaultRecorded) "option" else "not.default")

    var size = 3
    if (value != null) size++
    if (withDefaultRecorded) size++
    if (withProject) size++
    if (defaultProject) size++

    assertThat(event.data).hasSize(size)
    assertThat(event.data["component"]).isEqualTo("MyTestComponent")
    assertThat(event.data["type"]).isEqualTo(type)
    assertThat(event.data["name"]).isEqualTo(name)
    if (value != null) {
      assertThat(event.data["value"]).isEqualTo(value)
    }
    if (withDefaultRecorded) {
      assertThat(event.data["default"]).isEqualTo(false)
    }
    if (withProject) {
      assertThat(event.data).containsKey("project")
    }
    if (defaultProject) {
      assertThat(event.data["default_project"]).isEqualTo(true)
    }
  }

  private fun assertDefaultState(printer: TestFeatureUsageSettingsEventsPrinter, withProject: Boolean, defaultProject: Boolean) {
    assertThat(printer.result).hasSize(1)
    assertDefaultState(printer.result[0], "boolOption", false, "bool", withProject, defaultProject)
  }

  private fun assertDefaultState(event: LoggedComponentStateEvents,
                                 name: String,
                                 value: Any,
                                 type: String,
                                 withProject: Boolean,
                                 defaultProject: Boolean) {
    assertThat(event.group.id).isEqualTo("settings")
    assertThat(event.group.version).isGreaterThan(0)
    assertThat(event.id).isEqualTo("option")

    var size = 5
    if (withProject) size++
    if (defaultProject) size++

    assertThat(event.data).hasSize(size)
    assertThat(event.data["component"]).isEqualTo("MyTestComponent")
    assertThat(event.data["type"]).isEqualTo(type)
    assertThat(event.data["name"]).isEqualTo(name)
    assertThat(event.data["value"]).isEqualTo(value)
    assertThat(event.data["default"]).isEqualTo(true)
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
    assertThat(event.id).isEqualTo("invoked")

    var size = 1
    if (withProject) size++
    if (defaultProject) size++

    assertThat(event.data).hasSize(size)
    assertThat(event.data["component"]).isEqualTo("MyTestComponent")
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
        if (event.id == "invoked") {
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

  @Suppress("unused")
  private class ComponentStateWithNumerical(intOpt: Int = 0,
                                            longOpt: Long = 0,
                                            floatOpt: Float = 0.0F,
                                            doubleOpt: Double = 0.0,
                                            absIntOpt: Int = 0,
                                            absLongOpt: Long = 0,
                                            absFloatOpt: Float = 0.0F,
                                            absDoubleOpt: Double = 0.0,
                                            bool: Boolean = false,
                                            str: String = "string-option",
                                            list: List<Int> = ArrayList()) : ComponentState(bool, str, list) {
    @Attribute("int-option")
    val integerOption: Int = intOpt

    @Attribute("long-option")
    val longOption: Long = longOpt

    @Attribute("float-option")
    val floatOption: Float = floatOpt

    @Attribute("double-option")
    val doubleOption: Double = doubleOpt

    @Attribute("abs-int-option")
    @field:ReportValue
    val absIntegerOption: Int = absIntOpt

    @Attribute("abs-long-option")
    @field:ReportValue
    val absLongOption: Long = absLongOpt

    @Attribute("abs-float-option")
    @field:ReportValue
    val absFloatOption: Float = absFloatOpt

    @Attribute("abs-double-option")
    @field:ReportValue
    val absDoubleOption: Double = absDoubleOpt
  }
}