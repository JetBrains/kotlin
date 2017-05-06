# TensorFlow demo

Small Hello World calculation on the [TensorFlow](https://www.tensorflow.org/) backend, 
arranging simple operations into a graph and running it on a session.
Like other [TensorFlow clients](https://www.tensorflow.org/extend/language_bindings) 
(e. g. for [Python](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/python/client)), 
this example is built on top of the 
[TensorFlow C API](https://github.com/tensorflow/tensorflow/blob/r1.1/tensorflow/c/c_api.h), 
showing how a TensorFlow client in Kotlin/Native could look like.

## Installation

    ./build.sh

will install [TensorFlow for C](https://www.tensorflow.org/versions/r1.1/install/install_c) into `/opt/local` (if not yet done) and build the example.

    ./HelloTensorflow.kexe

will then run the example.

You may need to specify `LD_LIBRARY_PATH` or `DYLD_LIBRARY_PATH` to `/opt/local/lib` if the TensorFlow dynamic library cannot be found.