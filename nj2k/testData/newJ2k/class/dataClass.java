package lombok;

import lombok.Data;

public @interface Data {
}

@Data
public class Pojo {
    private final int foo;
    private final long bar;
    private final String baz;
}