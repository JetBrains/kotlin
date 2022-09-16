// EXPECTED_ERROR: This class does not have a constructor (1,2)

//import kotlin.jvm.JvmRepeatable

//@JvmRepeatable
annotation class Condition(val condition: String)

@Condition(condition = "value1")
@Condition(condition = "value2")
class A