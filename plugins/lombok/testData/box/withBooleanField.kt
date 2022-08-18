// ISSUE: KT-53451, KT-53370

// FILE: UserData.java
import lombok.*;

public class UserData {
    @With @Getter
    private final String name;

    @With @Getter
    private final boolean isSome;

    @With @Getter
    private final boolean hasFlag;

    @With @Getter
    private final boolean justAnother;

    public UserData(String name, boolean isSome, boolean hasFlag, boolean justAnother) {
        this.name = name;
        this.isSome = isSome;
        this.hasFlag = hasFlag;
        this.justAnother = justAnother;
    }
}

// FILE: main.kt
fun box(): String {
    val user = UserData("Bob", false, false, false)
    val modifiedUser = user
        .withName("Alex")
        .withSome(true)
        .withHasFlag(true)
        .withJustAnother(true)

    return if (modifiedUser.name == "Alex" && modifiedUser.isSome && modifiedUser.isHasFlag && modifiedUser.isJustAnother) {
        "OK"
    } else {
        "Error"
    }
}
