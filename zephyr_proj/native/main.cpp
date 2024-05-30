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
	try
	{
		LOG_WRN("%s", "started");
		__cxxabiv1::__cxa_guard_acquire(NULL);
		// libkn_symbols();
		// throw std::invalid_argument("sample exception");
		LOG_WRN("%s", "ended");
	}
	catch (std::exception &e)
	{
		LOG_WRN("exception: %s", e.what());
	}
}
