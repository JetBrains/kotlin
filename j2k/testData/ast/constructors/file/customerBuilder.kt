package org.test.customer
open class Customer(first : String?, last : String?) {
public val _firstName : String?
public val _lastName : String?
open public fun getFirstName() : String? {
return _firstName
}
open public fun getLastName() : String? {
return _lastName
}
open private fun doSmthBefore() : Unit {
}
open private fun doSmthAfter() : Unit {
}
{
doSmthBefore()
_firstName = first
_lastName = last
doSmthAfter()
}
}
open class CustomerBuilder() {
public var _firstName : String? = "Homer"
public var _lastName : String? = "Simpson"
open public fun WithFirstName(firstName : String?) : CustomerBuilder? {
_firstName = firstName
return this
}
open public fun WithLastName(lastName : String?) : CustomerBuilder? {
_lastName = lastName
return this
}
open public fun Build() : Customer? {
return Customer(_firstName, _lastName)
}
}
public open class User() {
class object {
open public fun main() : Unit {
var customer : Customer? = CustomerBuilder().WithFirstName("Homer")?.WithLastName("Simpson")?.Build()
System.out?.println(customer?.getFirstName())
System.out?.println(customer?.getLastName())
}
}
}