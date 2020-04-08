package target

import source.sourcePackFun

fun targetPackFun(){}


/* comment 1 */

fun foo() {
    sourcePackFun()
    targetPackFun()
}

/* comment 2 */

val bar = 10

/* comment 3 */
