package org.jetbrains.jet.j2k;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.psi.PsiJavaFile;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.j2k.visitors.ClassVisitor;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.jetbrains.jet.j2k.TestCaseBuilder.getTestDataPathBase;

/**
 * @author ignatov
 */
public class JavaToKotlinConverterTest extends LightDaemonAnalyzerTestCase {
  private final String myDataPath;
  private final String myName;

  public JavaToKotlinConverterTest(String dataPath, String name) {
    myDataPath = dataPath;
    myName = name;
  }

  @Override
  protected void runTest() throws Throwable {
    String javaPath = getTestDataPath() + File.separator + getTestFilePath();
    String kotlinPath = javaPath.replace(".jav", ".kt");

    final File kotlinFile = new File(kotlinPath);
    if (!kotlinFile.exists())
      writeStringToFile(kotlinFile, "");
    final String expected = readFileToString(kotlinFile);
    final File javaFile = new File(javaPath);
    final String javaCode = readFileToString(javaFile);

    String actual = "";
    if (javaFile.getParent().endsWith("/expression")) actual = expressionToKotlin(javaCode);
    else if (javaFile.getParent().endsWith("/statement")) actual = statementToKotlin(javaCode);
    else if (javaFile.getParent().endsWith("/method")) actual = methodToKotlin(javaCode);
    else if (javaFile.getParent().endsWith("/class")) actual = fileToKotlin(javaCode);
    else if (javaFile.getParent().endsWith("/file")) actual = fileToKotlin(javaCode);
    else if (javaFile.getParent().endsWith("/comp")) actual = fileToFileWithCompatibilityImport(javaCode);

    assert !actual.isEmpty() : "Specify what is it: file, class, method, statement or expression";

    final File tmp = new File(kotlinPath + ".tmp");
    if (!expected.equals(actual)) writeStringToFile(tmp, actual);
    if (expected.equals(actual) && tmp.exists()) //noinspection ResultOfMethodCallIgnored
      tmp.delete();

    Assert.assertEquals(expected, actual);
  }

  @NotNull
  String getTestFilePath() {
    return myDataPath + File.separator + myName + ".jav";
  }

  @NotNull
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

  @NotNull
  @Override
  public String getName() {
    return "test_" + myName;
  }

  @NotNull
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(TestCaseBuilder.suiteForDirectory(getTestDataPathBase(), "/ast", new TestCaseBuilder.NamedTestFactory() {
      @NotNull
      @Override
      public Test createTest(@NotNull String dataPath, @NotNull String name) {
        return new JavaToKotlinConverterTest(dataPath, name);
      }
    }));
    return suite;
  }

  private static void configureFromText(String text) throws IOException {
    configureFromFileText("test.java", text);
  }

  private static void setClassIdentifiers() {
    ClassVisitor c = new ClassVisitor();
    myFile.accept(c);
    Converter.clearClassIdentifiers();
    Converter.setClassIdentifiers(c.getClassIdentifiers());
  }

  @NotNull
  private static String fileToFileWithCompatibilityImport(String text) throws IOException {
    configureFromText(text);
    setClassIdentifiers();
    return prettify(Converter.fileToFileWithCompatibilityImport((PsiJavaFile) myFile).toKotlin());
  }

  @NotNull
  private static String fileToKotlin(String text) throws IOException {
    configureFromText(text);
    setClassIdentifiers();
    return prettify(Converter.fileToFile((PsiJavaFile) myFile).toKotlin());
  }

  @NotNull
  private static String methodToKotlin(String text) throws IOException {
    String result = fileToKotlin("final class C {" + text + "}")
      .replaceAll("class C\\(\\) \\{", "");
    result = result.substring(0, result.lastIndexOf("}"));
    return prettify(result);
  }

  @NotNull
  private static String statementToKotlin(String text) throws Exception {
    String result = methodToKotlin("void main() {" + text + "}");
    int pos = result.lastIndexOf("}");
    result = result.substring(0, pos).replaceFirst("fun main\\(\\) : Unit \\{", "");
    return prettify(result);
  }

  @NotNull
  private static String expressionToKotlin(String code) throws Exception {
    String result = statementToKotlin("Object o =" + code + "}");
    result = result.replaceFirst("var o : Any\\? =", "");
    return prettify(result);
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
}