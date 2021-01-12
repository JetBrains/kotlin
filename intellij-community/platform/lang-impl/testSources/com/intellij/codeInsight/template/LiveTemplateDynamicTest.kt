// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template

import com.intellij.codeInsight.template.impl.MacroCallNode
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.codeInsight.template.impl.VariableNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class LiveTemplateDynamicTest : LightPlatformCodeInsightTestCase() {
  fun testRegisterMacro() {
    val template = TemplateManager.getInstance(project).createTemplate("foo", "user", "\$FOO\$")
    template.addVariable("FOO", "foobar", "foobar", false)
    TemplateSettings.getInstance().addTemplate(template)
    Disposer.register(testRootDisposable, Disposable { TemplateSettings.getInstance().removeTemplate(template) })

    val variable = (template as TemplateImpl).variables.first()
    assertTrue(variable.expression is VariableNode)
    assertTrue(variable.defaultValueExpression is VariableNode)

    val disposable = Disposer.newDisposable()
    Disposer.register(testRootDisposable, disposable)
    Macro.EP_NAME.getPoint().registerExtension(FooBarMacro(), disposable)
    assertTrue(variable.expression is MacroCallNode)
    assertTrue(variable.defaultValueExpression is MacroCallNode)

    Disposer.dispose(disposable)
    assertTrue(variable.expression is VariableNode)
    assertTrue(variable.defaultValueExpression is VariableNode)
  }

  private class FooBarMacro : Macro() {
    override fun getName(): String = "foobar"

    override fun getPresentableName(): String = "Foo Bar"


    override fun calculateResult(params: Array<out Expression>, context: ExpressionContext?): Result? {
      return TextResult("Foo Bar")
    }
  }
}