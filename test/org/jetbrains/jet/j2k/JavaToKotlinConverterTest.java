package org.jetbrains.jet.j2k;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.psi.PsiJavaFile;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.jetbrains.jet.j2k.TestCaseBuilder.getTestDataPathBase;

/**
 * @author ignatov
 */
public class JavaToKotlinConverterTest extends LightDaemonAnalyzerTestCase {
  private String myDataPath;
  private String myName;

  public JavaToKotlinConverterTest(String dataPath, String name) {
    myDataPath = dataPath;
    myName = name;
  }

  private static String readFileAsString(String filePath)
    throws java.io.IOException {
    StringBuffer fileData = new StringBuffer(1000);
    BufferedReader reader = new BufferedReader(
      new FileReader(filePath));
    char[] buf = new char[1024];
    int numRead = 0;
    while ((numRead = reader.read(buf)) != -1) {
      String readData = String.valueOf(buf, 0, numRead);
      fileData.append(readData);
      buf = new char[1024];
    }
    reader.close();
    return fileData.toString();
  }

  @Override
  protected void runTest() throws Throwable {
    String javaPath = getTestDataPath() + File.separator + getTestFilePath();
    String kotlinPath = javaPath.replace(".jav", ".kt");

    String expected = readFileAsString(kotlinPath);

    String actual = "";
    if (javaPath.contains("/expression_mult/")) actual = expressionToKotlin(readFileAsString(javaPath));
    else if (javaPath.contains("/expression/")) actual = expressionToKotlin(readFileAsString(javaPath));
    else if (javaPath.contains("/statement/")) actual = statementToSingleLineKotlin(readFileAsString(javaPath));
    else if (javaPath.contains("/statement_mult/")) actual = statementToKotlin(readFileAsString(javaPath));

    else if (javaPath.contains("/method/")) actual = methodToSingleLineKotlin(readFileAsString(javaPath));
    else if (javaPath.contains("/method_mult/")) actual = methodToKotlin(readFileAsString(javaPath));

    else if (javaPath.contains("/class/")) actual = classToSingleLineKotlin(readFileAsString(javaPath));
    else if (javaPath.contains("/class_mult/")) actual = classToKotlin(readFileAsString(javaPath));

    else if (javaPath.contains("/file/")) actual = fileToSingleLineKotlin(readFileAsString(javaPath));
    else if (javaPath.contains("/file_mult/")) actual = fileToKotlin(readFileAsString(javaPath));

    assert !actual.equals("");
    Assert.assertEquals(expected, actual);
  }

  @NotNull
  protected String getTestFilePath() {
    return myDataPath + File.separator + myName + ".jav";
  }

  @Override
  protected String getTestDataPath() {
    return "testData";
  }

  private static Sdk jdkFromIdeaHome() {
    return new JavaSdkImpl().createJdk("JDK", "jre", true);
  }

  protected Sdk getProjectJDK() {
    return jdkFromIdeaHome();
  }

  @Override
  public String getName() {
    return "test_" + myName;
  }

  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(TestCaseBuilder.suiteForDirectory(getTestDataPathBase(), "/ast", true, new TestCaseBuilder.NamedTestFactory() {
      @NotNull
      @Override
      public Test createTest(@NotNull String dataPath, @NotNull String name) {
        return new JavaToKotlinConverterTest(dataPath, name);
      }
    }));
    return suite;
  }

  void configureFromText(String text) throws IOException {
    configureFromFileText("test.java", text);
  }

  protected String fileToKotlin(String text) throws IOException {
    configureFromText(text);
    return prettify(Converter.fileToFile((PsiJavaFile) myFile).toKotlin());
  }

  protected String fileToSingleLineKotlin(String text) throws IOException {
    return toSingleLine(fileToKotlin(text));
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

  protected String methodToKotlin(String text) throws IOException {
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
    result = result.replaceFirst("var o : Any\\? =", "");
    return prettify(result);
  }

  protected String expressionToSingleLineKotlin(String code) throws Exception {
    return toSingleLine(expressionToKotlin(code));
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
  public static String toSingleLine(String string) {
    return prettify(string.replaceAll("\n", " "));
  }
}