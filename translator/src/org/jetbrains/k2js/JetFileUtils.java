package org.jetbrains.k2js;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.File;
import java.io.IOException;

/**
 * @author Pavel Talanov
 */
public final class JetFileUtils {

    @NotNull
    private static JetCoreEnvironment environment = new JetCoreEnvironment(new Disposable() {

        @Override
        public void dispose() {
        }
    });

    @NotNull
    public static String loadFile(@NotNull String path) throws IOException {
        return doLoadFile(path);
    }

    @NotNull
    private static String doLoadFile(@NotNull String path) throws IOException {
        String text = FileUtil.loadFile(new File(path), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        return text;
    }

    @NotNull
    public static JetFile createPsiFile(@NotNull String name, @NotNull String text) {
        return (JetFile) createFile(name + ".jet", text);
    }

    @NotNull
    protected static JetFile loadPsiFile(@NotNull String name) {
        try {
            return createPsiFile(name, loadFile(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static PsiFile createFile(@NotNull String name, @NotNull String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        PsiFile result = ((PsiFileFactoryImpl) PsiFileFactory.getInstance(environment.getProject()))
                .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
        assert result != null;
        return result;
    }

}
