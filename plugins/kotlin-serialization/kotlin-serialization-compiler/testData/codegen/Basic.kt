// CURIOUS_ABOUT serialize, deserialize, write$Self, childSerializers, <init>, <clinit>
// WITH_STDLIB

import kotlinx.serialization.*

@Serializable
class User(val firstName: String, val lastName: String)

@Serializable
class OptionalUser(val user: User = User("", ""))

@Serializable
class ListOfUsers(val list: List<User>)