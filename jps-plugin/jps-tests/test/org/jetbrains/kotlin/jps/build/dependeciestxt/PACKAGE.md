Useful micro-language for concise description of project structure.
Mostly like the [DOT language](https://www.graphviz.org/doc/info/attrs.html).

## Example

```
c [common]
p1 [jvm]
p2 [jvm]

p1 -> c [expectedBy]
p2 -> c [expectedBy]
```

## Format

File contains declarations of modules and dependencies:
  - Module: `module_name [flag1, key1=value1, ...]`
  - Dependency: `source_module_name -> target_module_name [flag1, key1=value1, ...]`
  
Referring to undefined module is allowed (`jvm` module will be created at this case).
This modules can be defined after reference. Several declarations for same module is not allowed.

Supported module flags:
  - `common` (old MPP)
  - `sourceSetHolder`, `compilationAndSourceSetHolder` (new MPP)
  - `jvm` (default)
  - `js`
  - `edit`, `editJvm`, `editExcpetActual` - see jps-plugin/testData/incremental/multiplatform/multiModule/README.md
    
Supported dependency flags:
  - `compile` (default)
  - `test`
  - `runtime`
  - `provided`
  - `expectedBy` (old MPP)
  - `included` (new MPP)
  - `exproted`