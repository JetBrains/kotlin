let c = MyCls()
let ic = MyCls.MyInnerCls()
let ic2 = MyCls.MyInnerCls.MyInnerCls2()

var n: kotlin.Object? = nil

n = topLvlName()
n = topLvlName(dummy: Int32(123))
n = topLvlName

n = c.firstLvlName
n = c.firstLvlName()

n = ic.secondLvlName
n = ic.secondLvlName()

n = ic2.thirdLvlName
n = ic2.thirdLvlName()

n = namespace.topLvlName()
n = namespace.topLvlName(dummy: Int32(123))
n = namespace.topLvlName

n = namespace.secondary.topLvlName()
n = namespace.secondary.topLvlName(dummy: Int32(123))
n = namespace.secondary.topLvlName
