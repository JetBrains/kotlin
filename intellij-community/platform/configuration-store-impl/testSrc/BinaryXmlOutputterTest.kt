// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

class BinaryXmlOutputterTest {
  @Test
  fun noAttributes() {
    test("""<foo />""")
  }

  @Test
  fun textWithSpaces() {
    test("""
      <foo>
        <bar>Hello world</bar>
      </foo>""".trimMargin())
  }

  @Test
  fun attributes() {
    test("""
    <state>
      <component name="CopyrightManager">
        <copyright>
          <option name="myName" value="Foo" />
          <option name="notice" value="where" />
        </copyright>
      </component>
      <component name="InspectionProjectProfileManager">
        <profile version="1.0">
          <option name="myName" value="Project Default" />
          <inspection_tool class="AntDuplicateTargetsInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="AntMissingPropertiesFileInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="AntResolveInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="ArgNamesErrorsInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="ArgNamesWarningsInspection" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="AroundAdviceStyleInspection" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="DeclareParentsInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="EmptyEventHandler" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="PointcutMethodStyleInspection" enabled="false" level="WARNING" enabled_by_default="false" />
        </profile>
        <version value="1.0" />
      </component>
      <component name="ProjectLevelVcsManager" settingsEditedManually="false" />
      <component name="masterDetails">
        <states>
          <state key="Copyright.UI">
            <settings>
              <last-edited>Foo</last-edited>
              <splitter-proportions>
                <option name="proportions">
                  <list>
                    <option value="0.2" />
                  </list>
                </option>
              </splitter-proportions>
            </settings>
          </state>
          <state key="ProjectJDKs.UI">
            <settings>
              <last-edited>1.4</last-edited>
              <splitter-proportions>
                <option name="proportions">
                  <list>
                    <option value="0.2" />
                  </list>
                </option>
              </splitter-proportions>
            </settings>
          </state>
        </states>
      </component>
    </state>""")
  }

  @Test
  fun large() {
    test("""<ConnectionPersistentState>
      <option name="connections">
        <list>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="rootPath" value="rO0ABXQAFi9ob21lL2ZpbmtlbC9Eb3dubG9hZHM="/>
              </map>
            </option>
            <option name="fqn" value="com.jetbrains.bigdatatools.rfs.settings.local.RfsLocalConnectionData"/>
            <option name="groupId" value="RfsLocalConnectionGroup"/>
            <option name="innerId" value="Local@RfsLocalConnectionGroup@-3157430570869755376"/>
            <option name="name" value="Local"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="enabled" value="false"/>
            <option name="extended">
              <map>
                <entry key="defaultNotebookName" value="rO0ABXQAE0lERUFfUGx1Z2luL2RlZmF1bHQ="/>
                <entry key="enableBasicAuth" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="enableIntellijIntegration" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="enableProxy" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="hadoopVersion" value="rO0ABXQABTIuNy4z"/>
                <entry key="proxyAuthEnabled" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="proxyEnableType"
                       value="rO0ABX5yADVjb20uamV0YnJhaW5zLmJpZ2RhdGF0b29scy5jb25uZWN0aW9uLlByb3h5RW5hYmxlVHlwZQAAAAAAAAAAEgAAeHIADmphdmEubGFuZy5FbnVtAAAAAAAAAAASAAB4cHQACERJU0FCTEVE"/>
                <entry key="proxyHost" value="rO0ABXQAAA=="/>
                <entry key="proxyPort"
                       value="rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAABQ"/>
                <entry key="proxyType"
                       value="rO0ABX5yAC9jb20uamV0YnJhaW5zLmJpZ2RhdGF0b29scy5jb25uZWN0aW9uLlByb3h5VHlwZQAAAAAAAAAAEgAAeHIADmphdmEubGFuZy5FbnVtAAAAAAAAAAASAAB4cHQABEhUVFA="/>
                <entry key="scalaVersion" value="rO0ABXQABDIuMTE="/>
                <entry key="sparkVersion" value="rO0ABXQABTIuMi4w"/>
              </map>
            </option>
            <option name="fqn" value="com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData"/>
            <option name="groupId" value="ZeppelinConnections"/>
            <option name="innerId" value="Zeppelin connection@ZeppelinConnections@-7730330208164572094"/>
            <option name="name" value="Zeppelin connection"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
            <option name="port" value="0"/>
            <option name="uri" value="localhost:8080"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="baseDir" value="rO0ABXQAAA=="/>
                <entry key="bucket" value="rO0ABXQAKXRlc3QtNDJmZjZhZjgtNjJjYS0xMWVhLWJjNTUtMDI0MmFjMTMwMDAz"/>
                <entry key="jsonLocation" value="rO0ABXQAMS9ob21lL2ZpbmtlbC9OZXh0Y2xvdWQvYmR0LXN0Z24tYmVhMjlmNmIwNWJiLmpzb24="/>
              </map>
            </option>
            <option name="fqn" value="org.jetbrains.hdfsplugin.hdfs.settings.GCSConnectionData"/>
            <option name="groupId" value="GCSConnectionGroup"/>
            <option name="innerId" value="GS@GCSConnectionGroup@-6270932960199780104"/>
            <option name="name" value="GS"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="baseDir" value="rO0ABXQAAA=="/>
                <entry key="bucket" value="rO0ABXQAIWZ1cy1saW9uLXYzLWZpcmVob3NlLXBhcnF1ZXQtcHJvZA=="/>
                <entry key="jsonLocation" value="rO0ABXQAMS9ob21lL2ZpbmtlbC9OZXh0Y2xvdWQvYmR0LXN0Z24tYmVhMjlmNmIwNWJiLmpzb24="/>
              </map>
            </option>
            <option name="fqn" value="org.jetbrains.hdfsplugin.hdfs.settings.GCSConnectionData"/>
            <option name="groupId" value="GCSConnectionGroup"/>
            <option name="innerId" value="GS@GCSConnectionGroup@-7351000507156513454"/>
            <option name="name" value="GS"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="baseDir" value="rO0ABXQAAA=="/>
                <entry key="bucket" value="rO0ABXQAKXRlc3QtNTI1NjUwMzAtNGU2MC0xMWVhLTkwZDQtYjNjZmNlYmNjNGNh"/>
                <entry key="jsonLocation" value="rO0ABXQAMS9ob21lL2ZpbmtlbC9OZXh0Y2xvdWQvYmR0LXN0Z24tYmVhMjlmNmIwNWJiLmpzb24="/>
              </map>
            </option>
            <option name="fqn" value="org.jetbrains.hdfsplugin.hdfs.settings.GCSConnectionData"/>
            <option name="groupId" value="GCSConnectionGroup"/>
            <option name="innerId" value="GS@GCSConnectionGroup@3513935103951962930"/>
            <option name="name" value="GS"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="address" value="rO0ABXQAF2JpZ2RhdGEtYWxsLXVidW50dTo5MDAw"/>
                <entry key="authenticationType" value="rO0ABXQADEV4cGxpY2l0IHVyaQ=="/>
                <entry key="configsPath" value="rO0ABXQAAA=="/>
                <entry key="rootPath" value="rO0ABXQABi4uLy4uLw=="/>
                <entry key="timeout"
                       value="rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAADqY"/>
                <entry key="userName" value="rO0ABXQABmZpbmtlbA=="/>
              </map>
            </option>
            <option name="fqn" value="org.jetbrains.hdfsplugin.hdfs.settings.HdfsJavaConnectionData"/>
            <option name="groupId" value="HdfsJavaConnectionGroup"/>
            <option name="innerId" value="HDFS@HdfsJavaConnectionGroup@2301756648020102690"/>
            <option name="name" value="HDFS"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="defaultNotebookName" value="rO0ABXQAE0lERUFfUGx1Z2luL2RlZmF1bHQ="/>
                <entry key="enableBasicAuth" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="enableIntellijIntegration" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="enableProxy" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="hadoopVersion" value="rO0ABXQABTIuNy4z"/>
                <entry key="proxyAuthEnabled" value="rO0ABXNyABFqYXZhLmxhbmcuQm9vbGVhbs0gcoDVnPruAgABWgAFdmFsdWV4cAA="/>
                <entry key="proxyEnableType"
                       value="rO0ABX5yADVjb20uamV0YnJhaW5zLmJpZ2RhdGF0b29scy5jb25uZWN0aW9uLlByb3h5RW5hYmxlVHlwZQAAAAAAAAAAEgAAeHIADmphdmEubGFuZy5FbnVtAAAAAAAAAAASAAB4cHQACERJU0FCTEVE"/>
                <entry key="proxyHost" value="rO0ABXQAAA=="/>
                <entry key="proxyPort"
                       value="rO0ABXNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAABQ"/>
                <entry key="proxyType"
                       value="rO0ABX5yAC9jb20uamV0YnJhaW5zLmJpZ2RhdGF0b29scy5jb25uZWN0aW9uLlByb3h5VHlwZQAAAAAAAAAAEgAAeHIADmphdmEubGFuZy5FbnVtAAAAAAAAAAASAAB4cHQABEhUVFA="/>
                <entry key="scalaVersion" value="rO0ABXQABDIuMTE="/>
                <entry key="sparkVersion" value="rO0ABXQABTIuMi4w"/>
              </map>
            </option>
            <option name="fqn" value="com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData"/>
            <option name="groupId" value="ZeppelinConnections"/>
            <option name="innerId" value="Zeppelin connection@ZeppelinConnections@-6307657625482642774"/>
            <option name="name" value="Zeppelin connection"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
            <option name="port" value="0"/>
            <option name="uri" value="localhost:8080"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="activeAuthenticationType" value="rO0ABXQAE0J5IHVzZXJuYW1lIGFuZCBrZXk="/>
                <entry key="baseDir" value="rO0ABXQAAA=="/>
                <entry key="container" value="rO0ABXQADnRlc3Rjb250YWluZXIx"/>
                <entry key="endpoint" value="rO0ABXQAMWh0dHBzOi8vcGF2ZWxmaW5rZWxzaHRleW5qYi5ibG9iLmNvcmUud2luZG93cy5uZXQ="/>
              </map>
            </option>
            <option name="fqn" value="org.jetbrains.hdfsplugin.hdfs.settings.AzureConnectionData"/>
            <option name="groupId" value="AzureConnectionGroup"/>
            <option name="innerId" value="Azure@AzureConnectionGroup@3410539058694314920"/>
            <option name="name" value="Azure"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
          <ExtendedConnectionData>
            <option name="extended">
              <map>
                <entry key="activeAuthenticationType" value="rO0ABXQAFEJ5IGNvbm5lY3Rpb24gc3RyaW5n"/>
                <entry key="baseDir" value="rO0ABXQAAA=="/>
                <entry key="container" value="rO0ABXQADnRlc3Rjb250YWluZXIx"/>
                <entry key="endpoint" value="rO0ABXQALGh0dHBzOi8vcGF2ZWxmaW5rZWxzaHRleW5qYi5jb3JlLndpbmRvd3MubmV0"/>
              </map>
            </option>
            <option name="fqn" value="org.jetbrains.hdfsplugin.hdfs.settings.AzureConnectionData"/>
            <option name="groupId" value="AzureConnectionGroup"/>
            <option name="innerId" value="Azure@AzureConnectionGroup@291596504652932856"/>
            <option name="name" value="Azure"/>
            <option name="pluginId" value="com.intellij.bigdatatools"/>
          </ExtendedConnectionData>
        </list>
      </option>
    </ConnectionPersistentState>""")
  }

  private fun test(@Language("xml") xml: String) {
    val byteOut = BufferExposingByteArrayOutputStream()
    byteOut.use {
      serializeElementToBinary(JDOMUtil.load(xml), it)
    }

    val xmlAfter = JDOMUtil.writeElement(byteOut.toInputStream().use { deserializeElementFromBinary(it) })
    // fix formatting
    assertThat(JDOMUtil.writeElement(JDOMUtil.load(xml))).isEqualTo(xmlAfter)
  }
}