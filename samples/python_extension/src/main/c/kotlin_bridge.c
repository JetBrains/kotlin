/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Python.h>

#include "server_api.h"

#define __ server_symbols()->
#define T_(name) server_kref_demo_ ## name

// Note, that as we cache this in the global, and Kotlin/Native object references
// are currently thread local, we make this global a TLS variable.
#ifdef _MSC_VER
#define TLSVAR __declspec(thread)
#else
#define TLSVAR __thread
#endif

static TLSVAR server_kref_demo_Server server = { 0 };

static T_(Server) getServer(void) {
  if (!server.pinned) {
    server = __ kotlin.root.demo.Server.Server("the server");
  }
  return server;
}

static T_(Session) getSession(PyObject* args) {
   T_(Session) result = { 0 };
   long long pinned;
   if (PyArg_ParseTuple(args, "L", &pinned)) {
       result.pinned = (void*)(uintptr_t)pinned;
   }
   return result;
}

static PyObject* open_session(PyObject* self, PyObject* args) {
    PyObject *result = NULL;
    char* string_arg = NULL;
    int int_arg = 0;
    if (PyArg_ParseTuple(args, "is", &int_arg, &string_arg)) {
        T_(Session) session = __ kotlin.root.demo.Session.Session(string_arg, int_arg);
        result = Py_BuildValue("L", session.pinned);
    }
    return result;
}

static PyObject* close_session(PyObject* self, PyObject* args) {
    T_(Session) session = getSession(args);
    __ DisposeStablePointer(session.pinned);
    __ DisposeStablePointer(getServer().pinned);
    server.pinned = 0;
    return Py_BuildValue("L", 0);
}

static PyObject* greet_server(PyObject* self, PyObject* args) {
    T_(Server) server = getServer();
    T_(Session) session = getSession(args);
    const char* string = __ kotlin.root.demo.Server.greet(server, session);
    PyObject* result = Py_BuildValue("s", string);
    __ DisposeString(string);
    return result;
}

static PyObject* concat_server(PyObject* self, PyObject* args) {
    long long session_arg;
    char* string_arg1 = NULL;
    char* string_arg2 = NULL;
    PyObject* result = NULL;

    if (PyArg_ParseTuple(args, "Lss", &session_arg, &string_arg1, &string_arg2)) {
       T_(Server) server = getServer();
       T_(Session) session = { (void*)(uintptr_t)session_arg };
       const char* string = __ kotlin.root.demo.Server.concat(server, session, string_arg1, string_arg2);
       result = Py_BuildValue("s", string);
       __ DisposeString(string);
    } else {
        result = Py_BuildValue("s", NULL);
    }
    return result;
}

static PyObject* add_server(PyObject* self, PyObject* args) {
    long long session_arg;
    int int_arg1 = 0;
    int int_arg2 = 0;
    PyObject* result = NULL;

    if (PyArg_ParseTuple(args, "Lii", &session_arg, &int_arg1, &int_arg2)) {
       T_(Server) server = getServer();
       T_(Session) session = { (void*)(uintptr_t)session_arg };
       int sum = __ kotlin.root.demo.Server.add(server, session, int_arg1, int_arg2);
       result = Py_BuildValue("i", sum);
    } else {
        result = Py_BuildValue("i", 0);
    }
    return result;
}

static PyMethodDef kotlin_bridge_funcs[] = {
   { "open_session", (PyCFunction)open_session, METH_VARARGS, "Opens a session" },
   { "close_session", (PyCFunction)close_session, METH_VARARGS, "Closes the session" },
   { "greet_server", (PyCFunction)greet_server, METH_VARARGS, "Greeting service" },
   { "concat_server", (PyCFunction)concat_server, METH_VARARGS, "Concatenation service" },
   { "add_server", (PyCFunction)add_server, METH_VARARGS, "Addition service" },
   { NULL }
};

void initkotlin_bridge(void) {
   Py_InitModule3("kotlin_bridge", kotlin_bridge_funcs, "Kotlin/Native example module");
}
