// !specifyLocalVariableTypeByDefault: true
package test;

import org.jetbrains.annotations.Nullable;

public class Test {
  @Nullable String myStr = "String2";

  public Test(@Nullable String str) {
    myStr = str;
  }

  public void sout(@Nullable String str) {
    System.out.println(str);
  }

  @Nullable
  public String dummy(@Nullable String str) {
    return str;
  }

  public void test() {
    sout("String");
    @Nullable String test = "String2";
    sout(test);
    sout(dummy(test));

    new Test(test);
  }
}