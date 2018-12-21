/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import platform.zephyr.stm32f4_disco.*
import kotlinx.cinterop.*

fun blinky(value: Int) {

    val port = LED0_GPIO_CONTROLLER
    val led = LED0_GPIO_PIN
    var toggler = false
    val dev = device_get_binding(port)

    gpio_pin_configure(dev, led.convert(), GPIO_DIR_OUT)

    while (true) {
         /* Set pin to HIGH/LOW every 1 second */
         gpio_pin_write(dev, led.convert(), if (toggler) 1U else 0U);
         toggler = !toggler
         k_sleep(1000 * value);
   }
}

fun main() {
    blinky(1)
}
