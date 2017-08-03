#!/usr/bin/python

##
# Copyright 2010-2017 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# (lldb) command script import llvmDebugInfoC/src/scripts/konan_lldb.py
# (lldb) show_variable
#

import lldb
import commands
import optparse
import shlex

def show_variable(debugger, command, result, internal_dict):
    debugger.GetCommandInterpreter().HandleCommand('expr -- Konan_DebugPrint(%s)' % command, result)
    return result

def __lldb_init_module(debugger, internal_dict):
    debugger.HandleCommand('command script add -f konan_lldb.show_variable show_variable')

