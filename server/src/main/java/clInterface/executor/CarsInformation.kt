package clInterface.executor

import objects.Environment

class CarsInformation : CommandExecutor {

    override fun execute(command: String) {
        synchronized(Environment, {
            println(Environment.map.values)
        })
    }
}
