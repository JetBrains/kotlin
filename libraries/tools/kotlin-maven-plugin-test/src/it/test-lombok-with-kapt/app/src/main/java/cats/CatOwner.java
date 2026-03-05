package cats;

import lombok.Data;

/**
 * A cat owner with Lombok {@code @Data} (generates getters, setters, equals, hashCode, toString).
 */
@Data
public class CatOwner {
    private String name;
    private int catCount;
}