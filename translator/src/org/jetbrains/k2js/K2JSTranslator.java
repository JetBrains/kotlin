package org.jetbrains.k2js;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironmentException;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.utils.JetTestUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Talanov Pavel
 */
public final class K2JSTranslator {

    private final JetCoreEnvironment myEnvironment = new JetCoreEnvironment(new Disposable() {

        @Override
        public void dispose() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    });

    public K2JSTranslator() {
    }

    public void translate(String inputFile, String outputFile) {

        try {

            final File rtJar = CompileEnvironment.findRtJar(true);
            myEnvironment.addToClasspath(rtJar);

            JetNamespace namespace = loadPsiFile(inputFile).getRootNamespace();

            BindingContext bindingContext = JetTestUtils.analyzeNamespace(namespace,
                    JetControlFlowDataTraceFactory.EMPTY);

            NamespaceDescriptor namespaceDescriptor = BindingUtils.getNamespaceDescriptor(bindingContext, namespace);
            namespaceDescriptor.getMemberScope().getAllDescriptors();

        } catch (CompileEnvironmentException e) {
            System.out.println(e.getMessage());
        }
    }

    protected String loadFile(String path) throws IOException {
        return doLoadFile(path);
    }

    protected String doLoadFile(String path) throws IOException {
        String text = FileUtil.loadFile(new File(path), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        return text;
    }

    protected JetFile createPsiFile(String name, String text) {
        return (JetFile) createFile(name + ".jet", text);
    }

    protected JetFile loadPsiFile(String name) {
        try {
            return createPsiFile(name, loadFile(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return ((PsiFileFactoryImpl) PsiFileFactory.getInstance(myEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
    }

    protected void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    protected void prepareForTest(String name) throws IOException {
        // String text = loadFile(name + ".jet");
        // createAndCheckPsiFile(name, text);
    }

    protected void createAndCheckPsiFile(String name, String text) {
        //createCheckAndReturnPsiFile(name, text);
    }

}
