import lombok.AccessLevel
import lombok.Builder

@Builder(
    builderClassName = "SpecialUserBuilder",
    buildMethodName = "execute",
    builderMethodName = "createBuilder",
    toBuilder = true,
    access = AccessLevel.PACKAGE,
    setterPrefix = "set"
)
class User(
    val name: String,
    val age: Int,
)

fun testToBuilder(user: User) {
    val userBuilder: User.SpecialUserBuilder = user.toBuilder()
}

fun box(): String {
    val userBuilder: User.SpecialUserBuilder = User.createBuilder()
    val user = userBuilder
        .setName("John")
        .setAge(42)
        .execute()

    testToBuilder(user)

    return "OK"
}
