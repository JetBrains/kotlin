# TensorFlow demo

Small Hello World calculation on the [TensorFlow](https://www.tensorflow.org/) backend, 
arranging simple operations into a graph and running it on a session.
Like other [TensorFlow clients](https://www.tensorflow.org/extend/language_bindings) 
(e. g. for [Python](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/python/client)), 
this example is built on top of the 
[TensorFlow C API](https://github.com/tensorflow/tensorflow/blob/r1.1/tensorflow/c/c_api.h), 
showing how a TensorFlow client in Kotlin/Native could look like.

##Installation

[Install TensorFlow for C](https://www.tensorflow.org/versions/r1.1/install/install_c) into `/usr/local`.

Compile:

    ./build.sh

Run:

    ./HelloTensorflow.kexe

You may need to specify `LD_LIBRARY_PATH` or `DYLD_LIBRARY_PATH` to `/opt/local/lib` if TensorFlow dynamic library cannot be found.
