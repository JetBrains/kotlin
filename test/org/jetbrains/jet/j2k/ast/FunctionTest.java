package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class FunctionTest extends JetTestCaseBase {
  public void testEmptyVoidMethod() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("void main() {}"),
      "fun main() : Unit { }"
    );
  }

  public void testMethodClassType() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("String main() {}"),
      "fun main() : String? { }"
    );
  }

  public void testMethodPrimitiveType() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("int main() {}"),
      "fun main() : Int { }"
    );
  }

  public void testMethodPrimitiveType2() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("boolean main() {}"),
      "fun main() : Boolean { }"
    );
  }

  public void testMethodWithReturnStatement() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin("boolean isTrue() { return true; }"),
      "fun isTrue() : Boolean { return true }"
    );
  }
}