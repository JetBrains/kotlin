package lombok;

import lombok.Value;

public @interface Value {
}

@Value
public class Pojo {
    private int foo;
    private long bar;
    private String baz;
}