// TARGET_BACKEND: NATIVE
// WITH_PLATFORM_LIBS
// DISABLE_NATIVE: isAppleTarget=false
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.Network.NW_PARAMETERS_DISABLE_PROTOCOL
import platform.Network.nw_parameters_copy
import platform.Network.nw_parameters_create_secure_tcp
import platform.Network.nw_tcp_options_set_enable_keepalive

fun box(): String {
    // See KT-85508.
    val params = nw_parameters_create_secure_tcp(
        NW_PARAMETERS_DISABLE_PROTOCOL
    ) { tcpOptions ->
        nw_tcp_options_set_enable_keepalive(tcpOptions, true)
    }

    val paramsCopy = nw_parameters_copy(params) // Just a use site.
    return if (paramsCopy != null) "OK" else "FAIL: $params"
}
