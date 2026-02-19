import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import org.jetbrains.kotlinx.dataframe.schema.DataFrameSchema

@DataSchema
interface Name {
    val firstName: String
    val lastName: String
}

@DataSchema
interface Person {
    val name: DataRow<Name>
    val age: Int
    val city: String?
    val weight: Int?
    val isHappy: Boolean
}

@DataSchema
interface FirstNames {
    val firstName: String
    val secondName: String?
    val thirdName: String?
}

@DataSchema
interface Name2 {
    val firstName: DataRow<FirstNames>
    val lastName: String
}

@DataSchema
interface Person2 {
    val name: DataRow<Name2>
    val age: Int
    val city: String?
    val weight: Int?
    val isHappy: Boolean
}

val df = dataFrameOf("firstName", "lastName", "age", "city", "weight", "isHappy")(
    "Alice", "Cooper", 15, "London", 54, true,
    "Bob", "Dylan", 45, "Dubai", 87, true,
    "Charlie", "Daniels", 20, "Moscow", null, false,
    "Charlie", "Chaplin", 40, "Milan", null, true,
    "Bob", "Marley", 30, "Tokyo", 68, true,
    "Alice", "Wolf", 20, null, 55, false,
    "Charlie", "Byrd", 30, "Moscow", 90, true,
).group("firstName", "lastName").into("name").cast<Person>(verify = false)

val dfGroup = run {
    df.add {
        "secondName" from { "".takeIf { false } }
        "thirdName" from { "".takeIf { false } }
    }.move { name.firstName and secondName and thirdName }.under { name.firstName }.cast<Person2>(verify = false)
}

