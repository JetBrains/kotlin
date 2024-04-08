/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

data object PlusInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Add(args[0], args[1], pos, info, trafos)
    }
}

data object MinusInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Sub(args[0], args[1], pos, info, trafos)
    }
}

data object TimesInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Mul(args[0], args[1], pos, info, trafos)
    }
}

data object DivInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Div(args[0], args[1], pos, info, trafos)
    }
}

data object RemInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Mod(args[0], args[1], pos, info, trafos)
    }
}

data object NotBool : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 1)
        return Exp.Not(args[0], pos, info, trafos)
    }
}

data object AndBools : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.And(args[0], args[1], pos, info, trafos)
    }
}

data object OrBools : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Or(args[0], args[1], pos, info, trafos)
    }
}

data object ImpliesBools : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.Implies(args[0], args[1], pos, info, trafos)
    }
}

data object GeInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.GeCmp(args[0], args[1], pos, info, trafos)
    }
}

data object LeInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.LeCmp(args[0], args[1], pos, info, trafos)
    }
}

data object GtInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.GtCmp(args[0], args[1], pos, info, trafos)
    }
}

data object LtInts : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.LtCmp(args[0], args[1], pos, info, trafos)
    }
}

data object EqAny : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.EqCmp(args[0], args[1], pos, info, trafos)
    }
}

data object NeAny : Operator {
    override fun toFuncApp(args: List<Exp>, pos: Position, info: Info, trafos: Trafos): Exp {
        check(args.size == 2)
        return Exp.NeCmp(args[0], args[1], pos, info, trafos)
    }
}

