package kotlin.modules

import java.util.ArrayList

object AllModules : ThreadLocal<ArrayList<Module>>() {
    override fun initialValue() = ArrayList<Module>()
}
