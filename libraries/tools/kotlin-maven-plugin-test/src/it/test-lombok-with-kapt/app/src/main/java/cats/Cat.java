package cats;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * A cat defined in Java with Lombok-generated accessors.
 * Kotlin code should see the generated getters/setters via the Kotlin lombok plugin.
 */
@Getter @Setter @ToString
public class Cat {

    @NonNull
    private String name;
    private int lives;
    private boolean purring;

}