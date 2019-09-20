// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.macro;

import com.intellij.codeInsight.daemon.impl.quickfix.EmptyExpression;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.impl.TextExpression;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegExMacroTest {

  @Test
  public void calculateResult() {
    RegExMacro macro = new RegExMacro();
    assertNull(macro.calculateResult(createParams(null, "", ""), null, false));
    assertNull(macro.calculateResult(createParams("test", null, ""), null, false));
    assertNull(macro.calculateResult(createParams("test", "", null), null, false));
    assertEquals("DummyClass", macro.calculateResult(createParams("DummyClassTest", "(.*)Test", "$1"), null, false).toString());
  }

  @Test
  public void testTooFewParameters() {
    assertNull(new RegExMacro().calculateResult(new Expression[]{new TextExpression("one"), new TextExpression("two")}, null, false));
  }

  @NotNull
  private static Expression[] createParams(String value, String pattern, String replacement) {
    return new Expression[]{createExpression(value), createExpression(pattern), createExpression(replacement)};
  }

  @NotNull
  private static Expression createExpression(String value) {
    return value == null ? new EmptyExpression() : new TextExpression(value);
  }
}
