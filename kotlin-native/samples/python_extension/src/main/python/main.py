#!/usr/bin/python
#
# Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
# that can be found in the license/LICENSE.txt file.
#

import kotlin_bridge

session = kotlin_bridge.open_session(239, 'konan')

message = kotlin_bridge.greet_server(session)
print("Greet '{}'".format(message))

message = kotlin_bridge.concat_server(session, "Coding", "fun")
print("Concat '{}'".format(message))

message = kotlin_bridge.add_server(session, 1, 60)
print("Sum '{}'".format(message))


kotlin_bridge.close_session(session)

