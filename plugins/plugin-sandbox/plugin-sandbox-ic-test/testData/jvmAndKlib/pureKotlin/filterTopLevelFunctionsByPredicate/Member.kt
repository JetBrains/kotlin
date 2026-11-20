package test

import org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun

class Container {
    @TestTopLevelPrivateSuspendFun
    fun ignoredMember() {}
}
