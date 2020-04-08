// !FORCE_NOT_NULL_TYPES: false
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
package test;

import org.jetbrains.annotations.NotNull;

public class Test {
  @NotNull String myStr = "String2";

  public Test(@NotNull String str) {
    myStr = str;
  }

  public void sout(@NotNull String str) {
    // UNNECESSARY_NOT_NULL_ASSERTION heuristic does not work any more, instead we can skip generating !! altogether
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