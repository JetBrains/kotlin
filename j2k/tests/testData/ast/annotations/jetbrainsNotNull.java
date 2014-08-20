// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
package test;

import org.jetbrains.annotations.NotNull;

public class Test {
  @NotNull String myStr = "String2";

  public Test(@NotNull String str) {
    myStr = str;
  }

  public void sout(@NotNull String str) {
    System.out.println(str);
  }

  @NotNull
  public String dummy(@NotNull String str) {
    return str;
  }

  public void test() {
    sout("String");
    @NotNull String test = "String2";
    sout(test);
    sout(dummy(test));

    new Test(test);
  }
}