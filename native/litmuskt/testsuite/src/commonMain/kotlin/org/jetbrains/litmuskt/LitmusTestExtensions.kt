package org.jetbrains.litmuskt

//import org.jetbrains.litmuskt.generated.LitmusTestRegistry

//val LitmusTest<*>.alias get() = LitmusTestRegistry.getAlias(this)
//val LitmusTest<*>.qualifiedName get() = LitmusTestRegistry.getFQN(this)
//
//val LitmusTest<*>.javaClassName get() = alias.replace('.', '_')
//val LitmusTest<*>.javaFQN
//    get(): String {
//        val kotlinQN = qualifiedName
//        val lastDotIdx = kotlinQN.indexOfLast { it == '.' }
//        return kotlinQN.replaceRange(lastDotIdx..lastDotIdx, "_")
//    }
