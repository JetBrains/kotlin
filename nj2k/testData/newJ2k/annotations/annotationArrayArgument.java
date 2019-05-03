public @interface Ann {
    Inner[] value();
    InnerParam[] test1() default @InnerParam(C.class);
}

public @interface Inner {

}

public @interface InnerParam {
    Class<?> value();
}

@Ann(value = {@Inner, @Inner}, test1 = { @InnerParam(C.class) })
public class C {
}

@Ann({@Inner, @Inner})
public class D {
}

@Ann(value = @Inner)
public class E {
}

@Ann(value = {@Inner}, test1 = { @InnerParam(value = C.class) })
public class F {
}