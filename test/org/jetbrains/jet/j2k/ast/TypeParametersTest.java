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
      "fun max<T : Any?>(coll : Collection<out T?>?) : T? where T : Comparable<in T?>? { }"
    );
  }

  public void testMethodDoubleParametrizationWithTwoBounds() throws Exception {
    Assert.assertEquals(
      methodToSingleLineKotlin(
        "<T extends Object & Comparable<? super T>, K extends Node & Collection<? super K>> T max(Collection<? extends T> coll) {}"),
      "fun max<T : Any?, K : Node?>(coll : Collection<out T?>?) : T? where T : Comparable<in T?>?, K : Collection<in K?>? { }"
    );
  }

  public void testClassDoubleParametrizationWithTwoBoundsWithExtending() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class CC <T extends INode & Comparable<? super T>, K extends Node & Collection<? super K>> extends A {}"),
      "class CC<T : INode?, K : Node?> : A where T : Comparable<in T?>?, K : Collection<in K?>? { }"
    );
  }

  public void testTraitDoubleParametrizationWithTwoBoundsWithExtending() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin(
        "interface I <T extends INode & Comparable<? super T>, K extends Node & Collection<? super K>> extends II {}"),
      "trait I<T : INode?, K : Node?> : II where T : Comparable<in T?>?, K : Collection<in K?>? { }"
    );
  }

  public void testGenericClass() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class Comparable<T> {}"),
      "class Comparable<T> { }"
    );
  }

  public void testComplexExampleWithClassExtending() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin(
        "interface CommandHandler<T extends Command> {}"),
      "trait CommandHandler<T : Command?> { }"
    );
  }

  public void testClassParametrizationWithTwoBounds() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class C<T extends INode & Comparable<? super T>> {}"),
      "class C<T : INode?> where T : Comparable<in T?>? { }"
    );
  }

  public void testClassParametrizationWithTwoBoundsWithExtending() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin("final class C<T extends INode & Comparable<? super T>> extends A {}"),
      "class C<T : INode?> : A where T : Comparable<in T?>? { }"
    );
  }

  public void testComplexExampleWithClassMultiplyExtending() throws Exception {
    Assert.assertEquals(
      classToSingleLineKotlin(
        "interface CommandHandler<T extends INode, String> {}"),
      "trait CommandHandler<T : INode?, String> { }"
    );
  }
}
