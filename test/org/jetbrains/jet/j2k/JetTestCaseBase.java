package org.jetbrains.jet.j2k;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.psi.PsiJavaFile;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class JetTestCaseBase extends LightDaemonAnalyzerTestCase {
  private boolean checkInfos = false;
  private final String dataPath;
  private final String name;

  protected JetTestCaseBase() {
    this("", "");
  }

  private JetTestCaseBase(String dataPath, String name) {
    this.dataPath = dataPath;
    this.name = name;
  }

  public final JetTestCaseBase setCheckInfos(boolean checkInfos) {
    this.checkInfos = checkInfos;
    return this;
  }

  private static Sdk jdkFromIdeaHome() {
    return new JavaSdkImpl().createJdk("JDK", "compiler/testData/mockJDK-1.7/jre", true);
  }

  @Override
  protected String getTestDataPath() {
    return getTestDataPathBase();
  }

  private static String getTestDataPathBase() {
    return getHomeDirectory() + "/compiler/testData";
  }

  private static String getHomeDirectory() {
    return new File(PathManager.getResourceRoot(JetTestCaseBase.class, "/org/jetbrains/jet/JetTestCaseBase.class")).getParentFile().getParentFile().getParent();
  }

  @Override
  protected Sdk getProjectJDK() {
    return jdkFromIdeaHome();
  }

//  @Override
//  public String getName() {
//    return "test" + name;
//  }

//  @Override
//  protected void runTest() throws Throwable {
//    doTest(getTestFilePath(), true, checkInfos);
//  }

  @NotNull
  protected String getTestFilePath() {
    return dataPath + File.separator + name + ".jet";
  }

  protected String getDataPath() {
    return dataPath;
  }

  void configureFromText(String text) throws IOException {
    configureFromFileText("test.java", text);
  }

  @NotNull
  protected String classToKotlin(String text) throws IOException {
    configureFromText(text);

    PsiJavaFile javaFile = (PsiJavaFile) myFile;

    String result = prettify(Converter.fileToFile(javaFile).toKotlin()).replaceAll("namespace \\{", "");
    result = result.substring(0, result.lastIndexOf("}"));
    return prettify(result);
  }

  @NotNull
  protected String classToSingleLineKotlin(String text) throws IOException {
    return toSingleLine(classToKotlin(text));
  }

  String fileToKotlin(String text) throws IOException {
    configureFromText(text);
    return prettify(Converter.fileToFile((PsiJavaFile) myFile).toKotlin());
  }

  String methodToKotlin(String text) throws IOException {
    String result = classToKotlin("final class C {" + text + "}")
      .replaceAll("class C \\{", "");
    result = result.substring(0, result.lastIndexOf("}"));
    return prettify(result);
  }

  protected String methodToSingleLineKotlin(String text) throws IOException {
    return toSingleLine(methodToKotlin(text));
  }

  protected String statementToKotlin(String text) throws Exception {
    String result = methodToKotlin("void main() {" + text + "}");
    int pos = result.lastIndexOf("}");
    result = result.substring(0, pos).replaceFirst("fun main\\(\\) : Unit \\{", "");
    return prettify(result);
  }

  protected String statementToSingleLineKotlin(String code) throws Exception {
    return toSingleLine(statementToKotlin(code));
  }

  protected String expressionToKotlin(String code) throws Exception {
    String result = statementToKotlin("Object o =" + code + "}");
    result = result.replaceFirst("var o : Object\\? =", "");
    return prettify(result);
  }

  protected String expressionToSingleLineKotlin(String code) throws Exception {
    return toSingleLine(expressionToKotlin(code));
  }

  public interface NamedTestFactory {
    @NotNull
    Test createTest(@NotNull String dataPath, @NotNull String name);
  }

  @NotNull
  private static TestSuite suiteForDirectory(String baseDataDir, @NotNull final String dataPath, boolean recursive, @NotNull NamedTestFactory factory) {
    TestSuite suite = new TestSuite(dataPath);
    final String extension = ".jet";
    FilenameFilter extensionFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(extension);
      }
    };
    File dir = new File(baseDataDir + dataPath);
    FileFilter dirFilter = new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    };
    if (recursive) {
      File[] files = dir.listFiles(dirFilter);
      assert files != null : dir;
      List<File> subdirs = Arrays.asList(files);
      Collections.sort(subdirs);
      for (File subdir : subdirs) {
        suite.addTest(suiteForDirectory(baseDataDir, dataPath + "/" + subdir.getName(), recursive, factory));
      }
    }
    List<File> files = Arrays.asList(dir.listFiles(extensionFilter));
    Collections.sort(files);
    for (File file : files) {
      String fileName = file.getName();
      assert fileName != null;
      suite.addTest(factory.createTest(dataPath, fileName.substring(0, fileName.length() - extension.length())));
    }
    return suite;
  }

  @NotNull
  private static String prettify(String code) {
    if (code == null)
      return "";
    return code
      .trim()
      .replaceAll("\r\n", "\n")
      .replaceAll("\n+", "\n")
      .replaceAll("\n ", "\n")
      .replaceAll(" +", " ")
      .trim()
      ;
  }

  @NotNull
  static String toSingleLine(String string) {
    return prettify(string.replaceAll("\n", " "));
  }
}