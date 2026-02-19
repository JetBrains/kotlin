// FILE: User.java

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class User {
  private String name;
}


// FILE: test.kt
fun box(): String {
    val userBuilder: User.SpecialUserBuilder = User.builder()
    return "OK"
}

// FILE: lombok.config
lombok.builder.className=Special*Builder
