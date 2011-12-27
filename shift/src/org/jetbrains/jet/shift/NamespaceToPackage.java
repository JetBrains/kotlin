package org.jetbrains.jet.shift;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.pom.PomModel;
import com.intellij.pom.core.impl.PomModelImpl;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.compiler.AbstractCompileEnvironment;
import org.jetbrains.jet.compiler.CoreCompileEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.psi.JetTreeVisitor;
import org.picocontainer.*;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

import static org.jetbrains.jet.lexer.JetTokens.NAMESPACE_KEYWORD;

/**
 * @author abreslav
 */
public class NamespaceToPackage {

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        System.setProperty("java.awt.headless", "true");
        Disposable rootDisposable = new Disposable() {
            @Override
            public void dispose() {
            }
        };
        final JetCoreEnvironment environment = new JetCoreEnvironment(rootDisposable);

        MutablePicoContainer picoContainer = environment.getApplication().getPicoContainer();
        picoContainer.registerComponent(new ComponentAdapter() {
            @Override
            public Object getComponentKey() {
                return "com.intellij.openapi.progress.ProgressManager";
            }

            @Override
            public Class getComponentImplementation() {
                return ProgressManagerImpl.class;
            }

            @Override
            public Object getComponentInstance(PicoContainer picoContainer) throws PicoInitializationException, PicoIntrospectionException {
                return new ProgressManagerImpl(environment.getApplication());
            }

            @Override
            public void verify(PicoContainer picoContainer) throws PicoIntrospectionException {

            }

            @Override
            public void accept(PicoVisitor picoVisitor) {

            }
        });

        CoreCompileEnvironment compileEnvironment = new CoreCompileEnvironment();

        compileEnvironment.setJavaRuntime(AbstractCompileEnvironment.findRtJar(true));
            if (!compileEnvironment.initializeKotlinRuntime()) {
                System.out.println("foo");
            }
//        environment.getApplication().registerService(ProgressManager.class, new ProgressManagerImpl(environment.getApplication()));
        environment.getApplication().registerService(PomModel.class, new PomModelImpl(environment.getProject()));
//        environment.getApplication().registerService(Project.class, environment.getProject());

        final JetFile file = JetPsiFactory.createFile(environment.getProject(), "namespace foo\nclass A");
        final ASTNode packageKeyword = JetPsiFactory.createFile(environment.getProject(), "package foo").getRootNamespace().getHeader().getNode().findChildByType(NAMESPACE_KEYWORD);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        convert(file, packageKeyword);
                    }
                });
            }
        });

    }

    private static void convert(JetFile file, final ASTNode packageKeyword) {
        file.getRootNamespace().accept(new JetTreeVisitor<Void>() {
            @Override
            public Void visitNamespace(JetNamespace namespace, Void data) {
                ASTNode namespaceNode = namespace.getHeader().getNode();
                ASTNode token = namespaceNode.findChildByType(NAMESPACE_KEYWORD);
                namespaceNode.replaceChild(token, packageKeyword);
                return super.visitNamespace(namespace, data);
            }
        }, null);
        System.out.println("file = " + file.getText());
    }
}
