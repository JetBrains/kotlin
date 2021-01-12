/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.execution;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.execution.test.runner.events.TestEventXmlView;
import org.junit.Assert;
import org.junit.Test;

import java.util.Base64;

public abstract class TestEventXmlViewTestCase {

  @NotNull
  protected abstract TestEventXmlView load(@NotNull @Language("XML") final String xml) throws Exception;

  @Test(expected = Exception.class)
  public void test_empty_xml() throws Exception {
    load("");
  }

  @Test
  public void test_ijLog_empty() throws Exception {
    load("<ijLog/>");
  }

  @Test
  public void test_ijLog_event() throws Exception {
    load("<ijLog><event/></ijLog>");
  }

  @Test
  public void test_ijLog_event_id() throws Exception {
    final TestEventXmlView helper = load("<ijLog><event type='fpp'/></ijLog>");
    Assert.assertEquals("fpp", helper.getTestEventType());
  }

  @Test
  public void test_ijLog_event_test_ids() throws Exception {
    final TestEventXmlView helper = load("<ijLog><event><test id='aaa' parentId='bbb'/></event></ijLog>");
    Assert.assertEquals("aaa", helper.getTestId());
    Assert.assertEquals("bbb", helper.getTestParentId());
  }

  @Test
  public void test_ijLog_event_test_result_actualFile() throws Exception {
    final TestEventXmlView helper = load("<ijLog><event><test><result><actualFilePath>PAAATH</actualFilePath></result></test></event></ijLog>");
    Assert.assertEquals("PAAATH", helper.getEventTestResultActualFilePath());
  }

  @Test
  public void test_ijLog_event_test_result_actualCDATA() throws Exception {
    final TestEventXmlView helper = load("<ijLog><event><test><result><actual><![CDATA[PAAATH]]></actual></result></test></event></ijLog>");
    Assert.assertEquals("PAAATH", helper.getEventTestResultActual());
  }

  @Test
  public void performance_huge_message() throws Exception {
    final String outputMessage = Base64.getEncoder().encodeToString(new byte[256*1024]);

    @Language("XML") final String text =
      "<ijLog><event><test id='aaa' parentId='bbb'><event destination='2222'>" + outputMessage + "</event></test></event></ijLog>";

    parseSeveralTimes(text, 100);
  }

  @Test
  public void performance_small_message() throws Exception {
    @Language("XML") final String text =
      "<ijLog><event><test id='aaa' parentId='bbb'><event destination='2222'>intellijIdeaRulezzz!</event></test></event></ijLog>";

    parseSeveralTimes(text, 2000);
  }

  private void parseSeveralTimes(@NotNull final @Language("XML") String text, int tries) throws Exception {
    for(int i = 0; i < tries; i++) {
      final TestEventXmlView helper = load(text);
      Assert.assertTrue(helper.getTestId().length() > 0);
      Assert.assertTrue(helper.getTestEventTestDescription().length() > 0);
      Assert.assertTrue(helper.getTestEventTest().length() > 0);
    }
  }
}
