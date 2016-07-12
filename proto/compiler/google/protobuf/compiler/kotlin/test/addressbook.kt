class Person private constructor () {
  class PhoneNumber private constructor () {
    val number : kotlin.String? = null
    val type : PhoneType? = null

  }

  enum class PhoneType(val ord: Int) {
    MOBILE (0),
    HOME (1),
    WORK (2)
  }

  val name : kotlin.String? = null
  val id : Int? = null
  val email : kotlin.String? = null
  val phones : List <PhoneNumber>  = listOf()

}

class AddressBook private constructor () {
  val people : List <Person>  = listOf()

}

