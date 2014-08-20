package org.jetbrains.kotlin.doc.highlighter2

import java.io.File
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.kotlin.doc.*
import org.jetbrains.kotlin.doc.model.KModel
import org.jetbrains.kotlin.doc.model.SourceInfo
import org.jetbrains.kotlin.template.HtmlTemplate
import org.jetbrains.kotlin.template.escapeHtml

class Html2CompilerPlugin(private val compilerArguments: KDocArguments) : Doclet {

    private val docOutputRoot: File
    {
        val docOutputDir = compilerArguments.docConfig.docOutputDir
        if (docOutputDir.isEmpty()) {
            throw IllegalArgumentException("empty doc output dir")
        }
        docOutputRoot = File(docOutputDir)
    }

    private val srcOutputRoot = File(docOutputRoot, names.htmlSourceDirName)

    private val sourceDirs: List<File> =
        compilerArguments.freeArgs.orEmpty().map { path -> File(path).getCanonicalFile() }

    private val sourceDirPaths: List<String> = sourceDirs.map { d -> d.getPath() }

    private fun fileToWrite(psiFile: PsiFile): String {
        val file = File((psiFile.getVirtualFile() as CoreLocalVirtualFile).getPath()!!).getCanonicalFile()
        val filePath = file.getPath()
        for (sourceDirPath in sourceDirPaths) {
            if (filePath.startsWith(sourceDirPath) && filePath.length() > sourceDirPath.length()) {
                val relativePath = filePath.substring(sourceDirPath.length + 1)
                return relativePath
            }
        }
        throw Exception("$file is not a child of any source roots $sourceDirPaths")
    }

    override fun generate(model: KModel, outputDir: File) {
        srcOutputRoot.mkdirs()

        val css = javaClass<Html2CompilerPlugin>().getClassLoader()!!.getResourceAsStream(
                "org/jetbrains/kotlin/doc/highlighter2/hightlight.css")!!

        File(srcOutputRoot, "highlight.css").write { outputStream -> css.copyTo(outputStream) }

        for (sourceInfo in model.sourcesInfo) {
            processFile(sourceInfo)
        }
    }

    private fun processFile(sourceInfo: SourceInfo) {
        val psiFile = sourceInfo.psi
        val htmlPath = sourceInfo.htmlPath
        val htmlFile = File(srcOutputRoot, htmlPath)

        println("Generating $htmlFile")
        htmlFile.getParentFile()!!.mkdirs()

        // see http://maven.apache.org/jxr/xref/index.html

        val template = object : HtmlTemplate() {
            override fun render() {
                html {
                    head {
                        title("${psiFile.getName()} xref")
                        linkCssStylesheet("highlight.css")
                    }
                    body {
                        table(style = "white-space: pre; font-size: 10pt; font-family: Monaco, monospace") {
                            tr {
                                td(style = "margin-right: 1.5em; text-align: right") {
                                    val text = psiFile.getText()!!
                                    val lineCount =
                                        text.count { c -> c == '\n' } + (if (text.endsWith('\n')) 0 else 1)

                                    for (i in 1..lineCount) {
                                        val label = "$i"
                                        a(name = "$label", href = "#$label") {
                                            print(i)
                                        }
                                        println()
                                    }
                                }

                                td {
                                    fun classForPsi(psi: PsiElement): String? = when (psi) {
                                        is LeafPsiElement -> {
                                            val elementType = psi.getElementType()
                                            if (elementType == JetTokens.WHITE_SPACE)
                                                null
                                            else if (elementType == JetTokens.DOC_COMMENT)
                                                "hl-comment"
                                            else if (JetTokens.KEYWORDS.contains(elementType))
                                                "hl-keyword"
                                            else
                                                // TODO
                                                elementType.toString()
                                        }
                                        // TODO
                                        else -> psi.javaClass.getName()
                                    }

                                    for (t in splitPsi(psiFile)) {
                                        val text = t.first
                                        val psi = t.second
                                        span(className = classForPsi(psi)) {
                                            print(text.escapeHtml())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        template.renderTo(htmlFile)
    }

}
