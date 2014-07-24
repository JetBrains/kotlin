package demo 

import java.util.ArrayList

class KotlinGreetingJoiner() {

    val names = ArrayList<String?>()

    fun addName(name : String?): Unit{
        names.add(name)
    }
}

