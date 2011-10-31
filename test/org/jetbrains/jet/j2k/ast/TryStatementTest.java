package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class TryStatementTest extends JetTestCaseBase {
  public void testEmptyTryWithTwoCatchesWithoutFinally() throws Exception {
    Assert.assertEquals(
      statementToKotlin(
        "try {" +
          "} catch (Exception e) {" +
          "    println(1);" +
          "} catch (IOException e) {" +
          "    println(0);" +
          "}"),
      "try\n" +
        "{\n" +
        "}\n" +
        "catch (e : Exception?) {\n" +
        "println(1)\n" +
        "}\n" +
        "catch (e : IOException?) {\n" +
        "println(0)\n" +
        "}"
    );
  }

  public void testEmptyTryWithTwoCatchesWithEmptyFinally() throws Exception {
    Assert.assertEquals(
      statementToKotlin(
        "try {" +
          "} catch (Exception e) {" +
          "    println(1);" +
          "} catch (IOException e) {" +
          "    println(0);" +
          "} finally {}"),
      "try\n" +
        "{\n" +
        "}\n" +
        "catch (e : Exception?) {\n" +
        "println(1)\n" +
        "}\n" +
        "catch (e : IOException?) {\n" +
        "println(0)\n" +
        "}\n" +
        "finally\n" +
        "{\n" +
        "}"
    );
  }

  public void testEmptyTryWithTwoCatchesWithFinally() throws Exception {
    Assert.assertEquals(
      statementToKotlin(
        "try {" +
          "} catch (Exception e) {" +
          "    println(1);" +
          "} catch (IOException e) {" +
          "    println(0);" +
          "} finally {" +
          "    println(3);" +
          "}"),
      "try\n" +
        "{\n" +
        "}\n" +
        "catch (e : Exception?) {\n" +
        "println(1)\n" +
        "}\n" +
        "catch (e : IOException?) {\n" +
        "println(0)\n" +
        "}\n" +
        "finally\n" +
        "{\n" +
        "println(3)\n" +
        "}"
    );
  }

  public void testCommonCaseForTryStatement() throws Exception {
    Assert.assertEquals(
      statementToKotlin(
        "try {" +
          "    callMethod(params);" +
          "} catch (Exception e) {" +
          "    println(1);" +
          "} catch (IOException e) {" +
          "    println(0);" +
          "} finally {" +
          "    println(3);" +
          "}"),
      "try\n" +
        "{\n" +
        "callMethod(params)\n" +
        "}\n" +
        "catch (e : Exception?) {\n" +
        "println(1)\n" +
        "}\n" +
        "catch (e : IOException?) {\n" +
        "println(0)\n" +
        "}\n" +
        "finally\n" +
        "{\n" +
        "println(3)\n" +
        "}"
    );
  }

}
