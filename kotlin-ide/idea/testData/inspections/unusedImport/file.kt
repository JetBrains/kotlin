import java.io.* // unused
import java.sql.* // used

import java.util.HashMap // unused
import java.util.ArrayList // used

import java.unresolved.* // unresolved and unused
import java.net.Unresolved // unresolved and unused

import java.net.ConnectException as CE // unused
import java.net.ConnectException as ConExc // used

import RootPackageClass // unused because it's in the current package

fun foo(list: ArrayList<String>, p: RootPackageClass, e: ConExc) {
    list.add("")
    Date(4)
}

class RootPackageClass

// WITH_RUNTIME
// FULL_JDK
