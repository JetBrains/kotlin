/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
package org.jetbrains.jet.j2k;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.visitors.ClassVisitor;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;

public class JavaToKotlinTranslator {

    private static final Disposable DISPOSABLE = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    private JavaToKotlinTranslator() {
    }

    @Nullable
    private static PsiFile createFile(@NotNull String text) {
        JavaCoreProjectEnvironment javaCoreEnvironment = setUpJavaCoreEnvironment();
        return PsiFileFactory.getInstance(javaCoreEnvironment.getProject()).createFileFromText(
                "test.java", JavaLanguage.INSTANCE, text
        );
    }

    @Nullable
    static PsiFile createFile(@NotNull Project project, @NotNull String text) {
        return PsiFileFactory.getInstance(project).createFileFromText(
                "test.java", JavaLanguage.INSTANCE, text
        );
    }

    @NotNull
    static JavaCoreProjectEnvironment setUpJavaCoreEnvironment() {
        JavaCoreApplicationEnvironment applicationEnvironment = new JavaCoreApplicationEnvironment(DISPOSABLE);
        JavaCoreProjectEnvironment javaCoreEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);

        javaCoreEnvironment.addJarToClassPath(PathUtil.findRtJar());
        File annotations = findAnnotations();
        if (annotations != null && annotations.exists()) {
            javaCoreEnvironment.addJarToClassPath(annotations);
        }
        return javaCoreEnvironment;
    }

    @NotNull
    static String prettify(@Nullable String code) {
        if (code == null) {
            return "";
        }
        return code
                .trim()
                .replaceAll("\r\n", "\n")
                .replaceAll(" \n", "\n")
                .replaceAll("\n ", "\n")
                .replaceAll("\n+", "\n")
                .replaceAll(" +", " ")
                .trim()
                ;
    }

    @Nullable
    public static File findAnnotations() {
        ClassLoader classLoader = JavaToKotlinTranslator.class.getClassLoader();
        while (classLoader != null) {
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader loader = (URLClassLoader) classLoader;
                for (URL url : loader.getURLs())
                    if ("file".equals(url.getProtocol()) && url.getFile().endsWith("/annotations.jar")) {
                        return new File(url.getFile());
                    }
            }
            classLoader = classLoader.getParent();
        }
        return null;
    }

    static void setClassIdentifiers(@NotNull Converter converter, @NotNull PsiElement psiFile) {
        ClassVisitor c = new ClassVisitor();
        psiFile.accept(c);
        converter.clearClassIdentifiers();
        converter.setClassIdentifiers(c.getClassIdentifiers());
    }

    @NotNull
    static String generateKotlinCode(@NotNull String javaCode) {
        PsiFile file = createFile(javaCode);
        if (file != null && file instanceof PsiJavaFile) {
            Converter converter = new Converter(file.getProject());
            setClassIdentifiers(converter, file);
            return prettify(converter.fileToFile((PsiJavaFile) file).toKotlin());
        }
        return "";
    }

    @NotNull
    static String generateKotlinCodeWithCompatibilityImport(@NotNull String javaCode) {
        PsiFile file = createFile(javaCode);
        if (file != null && file instanceof PsiJavaFile) {
            Converter converter = new Converter(file.getProject());
            setClassIdentifiers(converter, file);
            return prettify(converter.fileToFileWithCompatibilityImport((PsiJavaFile) file).toKotlin());
        }
        return "";
    }

    public static void main(@NotNull String[] args) throws IOException {
        //noinspection UseOfSystemOutOrSystemErr
        PrintStream out = System.out;
        if (args.length == 1) {
            String kotlinCode = "";
            try {
                kotlinCode = generateKotlinCode(args[0]);
            } catch (Exception e) {
                out.println("EXCEPTION: " + e.getMessage());
            }
            if (kotlinCode.isEmpty()) {
                out.println("EXCEPTION: generated code is empty.");
            }
            else {
                out.println(kotlinCode);
            }
        }
        else {
            out.println("EXCEPTION: wrong number of arguments (should be 1).");
        }
    }

    public static String translateToKotlin(String code) {
        return generateKotlinCode(code);
    }
}
