package hair.test

import hair.sym.HairClass
import hair.sym.HairFunction
import hair.sym.HairType
import hair.sym.HairType.*

data class Fun(override val name: String) : HairFunction {
    override val resultHairType: HairType = INT
}

data class Cls(val name: String) : HairClass

