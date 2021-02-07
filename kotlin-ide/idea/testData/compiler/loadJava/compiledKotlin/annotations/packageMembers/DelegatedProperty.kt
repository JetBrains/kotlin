// SKIP_IN_RUNTIME_TEST because the anonymous object has no container and thus a non-local ClassId
// TODO: unskip

package test

import kotlin.reflect.KProperty

annotation class Anno

@Anno val x: Int by object {
    operator fun getValue(thiz: Any?, data: KProperty<*>): Nothing = null!!
}
