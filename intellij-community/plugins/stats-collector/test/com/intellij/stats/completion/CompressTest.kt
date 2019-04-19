// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.stats.network.service.GzipBase64Compressor
import org.apache.commons.codec.binary.Base64InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class CompressTest {
  @Test fun testSimple() {
    val text = """1470911998827	completion-stats	cd1f9318cd9f	46de626e4ea1	COMPLETION_STARTED	{
      |"completionListLength":1,"performExperiment":true,"experimentVersion":2,"completionListIds":[0],
      |"newCompletionListItems":[{"id":0,"length":4,"relevance":{"frozen":"true","sorter":"1","liftShorterClasses":"false",
      |"templates":"false","middleMatching":"false","liftShorter":"false","priority":"0.0","methodsChains":"00_0_2147483647",
      |"com.jetbrains.python.codeInsight.completion.PythonCompletionWeigher@5731acb0":"0","stats":"0","prefix":"-1133",
      |"kind":"localOrParameter","expectedType":"expected","recursion":"normal","nameEnd":"0","nonGeneric":"0","accessible":"NORMAL",
      |"simple":"0","explicitProximity":"0","proximity":"[referenceList\u003dunknown, samePsiMember\u003d2, explicitlyImported\u003d300,
      | javaInheritance\u003dnull, groovyReferenceListWeigher\u003dunknown, openedInEditor\u003dtrue, sameDirectory\u003dtrue,
      | sameLogicalRoot\u003dtrue, sameModule\u003d2, knownElement\u003d0, inResolveScope\u003dtrue, sdkOrLibrary\u003dfalse]",
      |"sameWords":"0","shorter":"0","grouping":"0"}}],"currentPosition":0,"userUid":"cd1f9318cd9f"}""".trimMargin()
    val encoded = GzipBase64Compressor.compress(text)
    val decoded = GZIPInputStream(Base64InputStream(ByteArrayInputStream(encoded))).reader().readText()
    assertThat(decoded).isEqualTo(text)
  }
}