package org.jetbrains.jet.j2k.ast;

import junit.framework.Assert;
import org.jetbrains.jet.j2k.JetTestCaseBase;

/**
 * @author ignatov
 */
public class TypeParametersTest extends JetTestCaseBase {
  public void testGenericParam() throws Exception {
    Assert.assertEquals(
      statementToKotlin("List<T> l;"),
      "var l : List<T?>?"
    );
  }

  public void testManyGenericParams() throws Exception {
    Assert.assertEquals(
      statementToKotlin("List<T, K, M> l;"),
      "var l : List<T?, K?, M?>?"
    );
  }

  public void testWhere() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin(
        "<T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {}"),
      "fun max<T : Object?>(coll : Collection<out T?>?) : T? where T : Comparable<in T?>? { }"
    );
  }

  public void testMethodDoubleParametrizationWithTwoBounds() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin(
        "<T extends Object & Comparable<? super T>, K extends Node & Collection<? super K>> T max(Collection<? extends T> coll) {}"),
      "fun max<T : Object?, K : Node?>(coll : Collection<out T?>?) : T? where T : Comparable<in T?>?, K : Collection<in K?>? { }"
    );
  }
}
