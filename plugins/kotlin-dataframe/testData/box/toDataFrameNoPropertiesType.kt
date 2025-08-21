import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

interface Animal {
    fun say(): String
}

class Dog(val name: String) : Animal {
    override fun say() = "bark"
}

class Cat(val name: String) : Animal {
    override fun say() = "meow"
}

fun box(): String {
    val animals: List<Animal> = listOf(Dog("dog"), Cat("cat"))
    val dfAnimals = animals.toDataFrame()
    val animalsCol: DataColumn<Animal> = dfAnimals.value
    animalsCol.print()

    val animalsNullable: List<Animal?> = listOf(Dog("dog"), Cat("cat"), null)
    val dfAnimalsNullable = animalsNullable.toDataFrame()
    val animalsNullableCol: DataColumn<Animal?> = dfAnimalsNullable.value
    animalsNullableCol.print()

    return "OK"
}
