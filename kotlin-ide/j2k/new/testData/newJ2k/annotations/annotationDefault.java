public @interface Ann {
    int i() default 1;
    int[] i2() default 1;
    int[] i3() default { 1 };
    Class<?> klass() default A.class;
    Class<?>[] klass2() default A.class;
    Class<?>[] klass3() default { A.class };
    Inner ann() default @Inner();
    Inner[] ann2() default @Inner;
    Inner[] ann3() default { @Inner, @Inner() };
}

public class A
public @interface Inner {

}
