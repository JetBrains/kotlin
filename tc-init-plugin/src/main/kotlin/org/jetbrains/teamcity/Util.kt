package org.jetbrains.teamcity

object Util {
    fun isInstance(clazz: Class<*>?, name: String): Boolean {
        if (clazz == null) return false
        if (clazz.name == name) return true
        if (clazz.name == "java.lang.Object") return false
        for (clazz0 in clazz.interfaces) {
            if (isInstance(clazz0, name)) return true
        }
        return isInstance(clazz.superclass, name)
    }

    fun getFlowIdForTask(taskPath: String): String {
        val flowIdPrefix = "%%teamcity%%-"
        return flowIdPrefix + taskPath.hashCode().toString()
    }
}
