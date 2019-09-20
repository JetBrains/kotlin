// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util

import com.intellij.codeEditor.printing.HTMLTextPainter
import com.intellij.codeEditor.printing.HtmlStyleManager
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.io.inputStream
import com.intellij.util.io.outputStream
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.CoreNodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.MutableDataSet
import org.junit.ClassRule
import org.junit.Test
import java.io.StringWriter
import java.nio.file.Paths

//private class MyApp : CommandLineApplication(true, false, true)

val packages = arrayOf(
  "platform/platform-api/src/com/intellij/util/io",
  "platform/util/src/com/intellij/util/xmlb/annotations"
)

class PackageDocGenerator {
  companion object {
    @ClassRule
    @JvmField
    val appRule = ProjectRule()
  }

  @Test
  fun convert() {
    if (UsefulTestCase.IS_UNDER_TEAMCITY) {
      return
    }

    main(PlatformTestUtil.getCommunityPath())
  }
}

private fun main(communityPath: String) {
//  PlatformTestCase.doAutodetectPlatformPrefix()
//  IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool(true)

//  MyApp()
//  PluginManagerCore.getPlugins()
//  ApplicationManagerEx.getApplicationEx().load(null)

  val options = MutableDataSet()
  val htmlStyleManager = HtmlStyleManager(isInline = true)
  val parser = Parser.builder(options).build()
  val renderer = HtmlRenderer.builder(options)
    .nodeRendererFactory { IntelliJNodeRenderer(options, htmlStyleManager) }
    .build()

  // see generatePackageHtmlJavaDoc - head.style cannot be used to define styles, so, we use inline style
  for (dir in packages) {
    val document = Paths.get(communityPath, dir, "readme.md").inputStream().reader().use { parser.parseReader(it) }
    Paths.get(communityPath, dir, "package.html").outputStream().bufferedWriter().use { writer ->
      if (htmlStyleManager.isInline) {
        renderer.render(document, writer)
      }
      else {
        val data = renderer.render(document)
        writer.append("<html>\n<head>\n")
        htmlStyleManager.writeStyleTag(writer, isUseLineNumberStyle = false)
        writer.append("</head>\n<body>\n")
        writer.write(data)
        writer.append("</body>\n</html>")
      }
    }
  }

//  System.exit(0)
}

private class IntelliJNodeRenderer(options: DataHolder, private val htmlStyleManager: HtmlStyleManager) : CoreNodeRenderer(options) {
  override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
    val set = LinkedHashSet<NodeRenderingHandler<*>>()
    set.add(NodeRenderingHandler(FencedCodeBlock::class.java) { node, context, html -> renderCode(node, context, html) })
    set.addAll(super.getNodeRenderingHandlers().filter { it.nodeType != FencedCodeBlock::class.java })
    return set
  }

  fun renderCode(node: FencedCodeBlock, context: NodeRendererContext, html: HtmlWriter) {
    html.line()
    //  html.srcPosWithTrailingEOL(node.chars).withAttr().tag("pre").openPre()
    //  html.srcPosWithEOL(node.contentChars).withAttr(CODE_CONTENT).tag("code")

    val writer = StringWriter()
    val project = ProjectManager.getInstance().defaultProject
    val psiFileFactory = PsiFileFactory.getInstance(project)
    runReadAction {
      val psiFile = psiFileFactory.createFileFromText(getLanguage(node), node.contentChars.normalizeEOL())
      val htmlTextPainter = HTMLTextPainter(psiFile, project, htmlStyleManager, false, false)

      writer.use {
        htmlTextPainter.paint(null, writer, false)
      }
    }

    html.rawIndentedPre(writer.buffer)

    //  html.tag("/code")
    //  html.tag("/pre").closePre()
    html.lineIf(context.htmlOptions.htmlBlockCloseTagEol)
  }
}

fun getLanguage(node: FencedCodeBlock): Language {
  val info = node.info
  if (info.isNotNull && !info.isBlank) run {
    val space = info.indexOf(' ')
    val language = (if (space == -1) info else info.subSequence(0, space)).unescape()
    val languageId = if (language == "kotlin") language else language.toUpperCase()
    return Language.findLanguageByID(languageId) ?: throw Exception("Cannot find language ${language}")
  }
  throw Exception("Please specify code block language")
}