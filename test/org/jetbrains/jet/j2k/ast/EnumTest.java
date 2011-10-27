package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class EnumTest extends JetTestCaseBase {
  public void testEmptyEnum() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("enum A {}"),
      "enum A { }"
    );
  }

//  public void testTypeSafeEnum() throws Exception {
//    Assert.assertEquals(
//      classToKotlin("enum Coin { PENNY, NICKEL, DIME, QUARTER; }"),
//      "enum Coin {\n" +
//        "PENNY\n" +
//        "NICKEL\n" +
//        "DIME\n" +
//        "QUARTER\n" +
//        "}"
//    );
//  }
}
