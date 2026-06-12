package hair.sym

interface HairFunction {
    val name: String
    val resultHairType: HairType
    val parameterTypes: List<HairType>
}