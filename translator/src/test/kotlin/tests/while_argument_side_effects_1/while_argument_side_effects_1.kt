object while_argument_side_effects_1_Counter{
    var cnt = 0
    fun isEnd():Boolean{
        cnt += 1
        return cnt < 10
    }
}

fun while_argument_side_effects_1():Int{
    var iter = 0
    while(while_argument_side_effects_1_Counter.isEnd()){
        iter += 1
    }
    return iter+ 1009*while_argument_side_effects_1_Counter.cnt
}