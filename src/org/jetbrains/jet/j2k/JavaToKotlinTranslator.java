/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.core.JavaCoreEnvironment;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.util.Function;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.visitors.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author ignatov
 */
public class JavaToKotlinTranslator {
  private JavaToKotlinTranslator() {
  }

  @Nullable
  protected static PsiFile createFile(@NonNls String name, String text) {
    JavaCoreEnvironment javaCoreEnvironment = new JavaCoreEnvironment(new Disposable() {
      @Override
      public void dispose() {
      }
    });

    javaCoreEnvironment.addToClasspath(findRtJar(true));
    File annotations = findAnnotations();
    if (annotations != null && annotations.exists()) {
      javaCoreEnvironment.addToClasspath(annotations);
    }
    return PsiFileFactory.getInstance(javaCoreEnvironment.getProject()).createFileFromText(
      name, JavaLanguage.INSTANCE, text
    );
  }

  @NotNull
  private static String prettify(@Nullable String code) {
    if (code == null)
      return "";
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

  public static File findRtJar(boolean failOnError) {
    String javaHome = System.getenv("JAVA_HOME");
    File rtJar;
    if (javaHome == null) {
      rtJar = findActiveRtJar(failOnError);

      if (rtJar == null && failOnError) {
        throw new SetupJavaCoreEnvironmentException("JAVA_HOME environment variable needs to be defined");
      }
    } else {
      rtJar = findRtJar(javaHome);
    }

    if ((rtJar == null || !rtJar.exists()) && failOnError) {
      rtJar = findActiveRtJar(failOnError);

      if ((rtJar == null || !rtJar.exists())) {
        throw new SetupJavaCoreEnvironmentException("No rt.jar found under JAVA_HOME=" + javaHome);
      }
    }
    return rtJar;
  }

  private static File findRtJar(String javaHome) {
    File rtJar = new File(javaHome, "jre/lib/rt.jar");
    if (rtJar.exists()) {
      return rtJar;
    }

    File classesJar = new File(new File(javaHome).getParentFile().getAbsolutePath(), "Classes/classes.jar");
    if (classesJar.exists()) {
      return classesJar;
    }
    return null;
  }

  @Nullable
  private static File findAnnotations() {
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    if (systemClassLoader instanceof URLClassLoader) {
      URLClassLoader loader = (URLClassLoader) systemClassLoader;
      for (URL url : loader.getURLs())
        if ("file".equals(url.getProtocol()) && url.getFile().endsWith("/annotations.jar"))
          return new File(url.getFile());
    }
    return null;
  }

  public static File findActiveRtJar(boolean failOnError) {
    ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    if (systemClassLoader instanceof URLClassLoader) {
      URLClassLoader loader = (URLClassLoader) systemClassLoader;
      for (URL url : loader.getURLs()) {
        if ("file".equals(url.getProtocol())) {
          if (url.getFile().endsWith("/lib/rt.jar")) {
            return new File(url.getFile());
          }
          if (url.getFile().endsWith("/Classes/classes.jar")) {
            return new File(url.getFile()).getAbsoluteFile();
          }
        }
      }
      if (failOnError) {
        throw new SetupJavaCoreEnvironmentException("Could not find rt.jar in system class loader: " + StringUtil.join(loader.getURLs(), new Function<URL, String>() {
          @Override
          public String fun(URL url) {
            return url.toString() + "\n";
          }
        }, ", "));
      }
    } else if (failOnError) {
      throw new SetupJavaCoreEnvironmentException("System class loader is not an URLClassLoader: " + systemClassLoader);
    }
    return null;
  }

  private static void setClassIdentifiers(PsiElement psiFile) {
    ClassVisitor c = new ClassVisitor();
    psiFile.accept(c);
    Converter.clearClassIdentifiers();
    Converter.setClassIdentifiers(c.getClassIdentifiers());
  }

  static String generateKotlinCode(String arg) {
    PsiFile file = createFile("test.java", arg);
    if (file != null && file instanceof PsiJavaFile) {
      setClassIdentifiers(file);
      return prettify(Converter.fileToFile((PsiJavaFile) file).toKotlin());
    }
    return "";
  }

  public static void main(String[] args) throws IOException {
    if (args.length > 0) {
      //noinspection UseOfSystemOutOrSystemErr
      System.out.println(generateKotlinCode(args[0]));
    }
  }
}
