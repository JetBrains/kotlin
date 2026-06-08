# test-instrumenter

A Java Agent for instrumenting tests, mainly used for undeclared inputs checking (see `test-inputs-check-v2`).

## Debugging

If you want to see debug logs and stack traces from the JVM, add this line to your `local.properties`:

```
test.instrumenter.debug=true
```

## System properties

The following system properties are recognized:

| System property                          | Description                                       | Source                 |
|------------------------------------------|---------------------------------------------------|------------------------|
| `test.instrumenter.debug`                | Enable/disable debug logging and JVM stack traces | `local.properties`     |
| `test.instrumenter.inputs.check.enabled` | Enable/disable inputs checking instrumentation    | `test-inputs-check-v2` |
| `test.instrumenter.root.dir`             | Root dir of kotlin.git                            | `test-inputs-check-v2` |
| `test.instrumenter.build.dir`            | Build dir of the project executing tests          | `test-inputs-check-v2` |
| `test.instrumenter.declared.inputs.file` | Path to file containg list of declared inputs     | `test-inputs-check-v2` |
