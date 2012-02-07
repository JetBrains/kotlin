package org.jetbrains.k2js.utils;

import com.google.common.io.ByteStreams;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class JetFileUtils {

    @NotNull
    public static String loadFile(@NotNull String path) throws IOException {
        return doLoadFile(path);
    }

    @NotNull
    private static String doLoadFile(@NotNull String path) throws IOException {
        path = path.replace("\\", "/");
        InputStreamReader reader = new InputStreamReader(JetFileUtils.class.getResourceAsStream(path));
        StringBuilder response = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(reader);

        String tmp;
        while ((tmp = bufferedReader.readLine()) != null) {
            response.append(tmp);
            response.append(System.getProperty("line.separator"));
        }

        reader.close();
        String text =  response.toString();
//        String text = FileUtil.loadFile(new File(path), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        return text;
    }

    @NotNull
    public static JetFile createPsiFile(@NotNull String name,
                                        @NotNull String text,
                                        @NotNull Project project) {
        return (JetFile) createFile(name + ".jet", text, project);
    }

    @NotNull
    public static JetFile loadPsiFile(@NotNull String name, @NotNull Project project) {
        try {
            return createPsiFile(name, loadFile(name), project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static PsiFile createFile(@NotNull String name, @NotNull String text, @NotNull Project project) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        PsiFile result = ((PsiFileFactoryImpl) PsiFileFactory.getInstance(project))
                .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
        assert result != null;
        return result;
    }


    @NotNull
    public static List<JetFile> createPsiFileList(@NotNull List<String> inputFiles,
                                                  @NotNull Project project) {
        List<JetFile> psiFiles = new ArrayList<JetFile>();
        for (String inputFile : inputFiles) {
            psiFiles.add(JetFileUtils.loadPsiFile(inputFile, project));
        }
        return psiFiles;
    }
}
