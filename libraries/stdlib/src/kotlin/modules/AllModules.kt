package kotlin.modules

import java.util.ArrayList

public object AllModules : ThreadLocal<ArrayList<Module>>() {
    override fun initialValue() = ArrayList<Module>()
}
