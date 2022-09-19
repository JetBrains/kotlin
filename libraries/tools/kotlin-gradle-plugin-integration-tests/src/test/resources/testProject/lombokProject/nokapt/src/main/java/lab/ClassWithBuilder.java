package lab;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Builder(setterPrefix = "with")
@Data
@Getter
public class ClassWithBuilder {
    @Singular
    private ImmutableList<Integer> stars;
}
