package lab2;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class SomePojo {

    @NonNull
    private String name;
    private int age;

    private boolean human;

}
