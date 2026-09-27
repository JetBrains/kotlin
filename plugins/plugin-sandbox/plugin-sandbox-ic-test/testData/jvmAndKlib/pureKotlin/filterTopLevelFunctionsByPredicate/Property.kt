package test

import org.jetbrains.kotlin.plugin.sandbox.TestTopLevelPrivateSuspendFun

@TestTopLevelPrivateSuspendFun
val ignoredProperty: Int get() = 1
