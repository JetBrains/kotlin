package lombok;

import lombok.Data;

public @interface Data {
}

@Data
public class Pojo {
    private int foo;
    private long bar;
    private String baz;
}w