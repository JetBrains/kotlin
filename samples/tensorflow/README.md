# TensorFlow demo

Small Hello World calculation on the [TensorFlow](https://www.tensorflow.org/) backend,
arranging simple operations into a graph and running it on a session.
Like other [TensorFlow clients](https://www.tensorflow.org/extend/language_bindings)
(e. g. for [Python](https://github.com/tensorflow/tensorflow/tree/master/tensorflow/python/client)),
this example is built on top of the
[TensorFlow C API](https://github.com/tensorflow/tensorflow/blob/r1.1/tensorflow/c/c_api.h),
showing how a TensorFlow client in Kotlin/Native could look like.

## Installation

    ./downloadTensorflow.sh

will install [TensorFlow for C](https://www.tensorflow.org/versions/r1.1/install/install_c) into
`$HOME/.konan/third-party/tensorflow` (if not yet done). One may override the location of
`third-party/tensorflow` by setting the `KONAN_DATA_DIR` environment variable.

To build use `../gradlew assemble`.

Then run

    ../gradlew runProgram

Alternatively you can run the artifact directly through

    ./build/bin/tensorflow/main/release/executable/tensorflow.kexe

You may need to specify `LD_LIBRARY_PATH` or `DYLD_LIBRARY_PATH` to `$HOME/.konan/third-party/tensorflow/lib`
if the TensorFlow dynamic library cannot be found.
