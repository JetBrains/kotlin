// FIR_IDENTICAL
// FILE: User.java

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class User {
  private String name;
}


// FILE: test.kt
fun test() {
    val userBuilder: User.SpecialUserBuilder = User.builder()
}

// FILE: lombok.config
lombok.builder.className=Special*Builder
