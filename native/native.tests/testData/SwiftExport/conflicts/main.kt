val topLvlName = "123"
fun topLvlName() = "123"
fun topLvlName(dummy: Int) = "123"

public class MyCls {
    public class MyInnerCls {
        public class MyInnerCls2 {
            val thirdLvlName = "123"
            fun thirdLvlName() = "123"
        }
        val secondLvlName = "123"
        fun secondLvlName() = "123"
    }

    val firstLvlName = "123"
    fun firstLvlName() = "123"
}
