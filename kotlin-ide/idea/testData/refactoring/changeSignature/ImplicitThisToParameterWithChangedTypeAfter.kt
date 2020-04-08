open class Older { fun upper() {} }
class Younger : Older()
public fun outer(younger: Older) {
    younger.upper()
}