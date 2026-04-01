package cats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A hand-written Java class (no Lombok) to verify Kotlin sees nullability annotations correctly.
 */
public class CatShelter {

    @Nullable
    public Cat findCatByName(String name) {
        return null;
    }

    @NotNull
    public String getShelterName() {
        return "Happy Paws";
    }
}