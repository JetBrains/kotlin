/*
 * Copyright (c) 2012-2014 Wind River Systems, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <libkn.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdexcept>
#include <iostream>

#include <cxxabi.h>

#include <zephyr/logging/log.h>
LOG_MODULE_REGISTER(main, LOG_LEVEL_WRN);

int main(void)
{
	libkn_symbols()->kotlin.root.example.get_globalString();
	LOG_WRN("%s", "ended");
}
