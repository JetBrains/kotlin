package test.kotlin

import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.config.CompilerConfiguration
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.JetLanguage
import org.jetbrains.jet.utils.PathUtil
import org.jetbrains.kotlin.doc.highlighter2.splitPsi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PsiUtilsTest {

    val rootDisposable = object : Disposable {
        public override fun dispose() {
        }
    }

    private var environment: JetCoreEnvironment? = null

    [Before]
    fun before() {
        System.setProperty("java.awt.headless", "true")

        val configuration = CompilerConfiguration()
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, PathUtil.findRtJar())
        environment = JetCoreEnvironment(rootDisposable, configuration)
    }

    [After]
    fun after() {
        Disposer.dispose(rootDisposable)
    }

    private fun createFile(content: String): JetFile {
        val virtualFile = LightVirtualFile("file.kt", JetLanguage.INSTANCE, content);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return (PsiFileFactory.getInstance(environment!!.getProject()) as PsiFileFactoryImpl)
                .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false) as JetFile
    }

    [Test]
    fun splitPsi() {
        val file = createFile("class Foo")
        val items: List<String> = splitPsi(file).map { t -> t.first }
        Assert.assertEquals(arrayList("class", " ", "Foo"), items)
    }

}
