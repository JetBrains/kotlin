# Module kotlin-gradle-plugin

`kotlin-gradle-plugin` artifact provides multiple plugins.

### kotlin

How to apply:
```
apply plugin: 'kotlin'
```

Tasks:
| Name | Type | Description
|------------------------------|--------------------|---------------|
| compileKotlin                | [KotlinJvmCompile] | A task is created for `main` source set |
| compileTestKotlin            | [KotlinJvmCompile] | A task is created for `test` source set |
| compile*SourceSetName*Kotlin | [KotlinJvmCompile] | A task is created for each additional source set |

Each [KotlinJvmCompile] task provides `kotlinOptions` ([KotlinJvmOptions]) extension:
```
compileKotlin {
    kotlinOptions {
        noStdlib = true
    }
    // kotlinOptions.noStdlib = true
}
```

### kotlin-android

A plugin that should be used for Android development.

How to apply:
```
apply plugin: 'kotlin-android'
```

Tasks:
| Name | Type | Description
|------------------------------|--------------------|---------------|
| compile*VariantName*Kotlin | [KotlinJvmCompile] | A task is created for each variant |

Note that tasks are created after evaluation, so all references to tasks should be done in `afterEvaluate` section:
```
afterEvaluate {
    compileDebugKotlin {
        kotlinOptions {
            noStdlib = true
        }
    }
}
```

Android plugin also adds `kotlinOptions` extension to `android` section to set options for all kotlin tasks:
```
android {
    kotlinOptions {
        noStdlib = true
    }
}
```

Task's `kotlinOptions` "override" ones in `android` section:
```
android {
    kotlinOptions {
        noStdlib = true
    }
}

afterEvaluate {
    compileDebugKotlin {
        kotlinOptions {
            noStdlib = false
        }
    }
}
// compileProductionKotlin.noStdlib == true
// compileDebugKotlin.noStdlib == false
```

### kotlin2js

How to apply:
```
apply plugin: 'kotlin2js'
```

Tasks:
| Name | Type | Description
|--------------------------------|--------------------|---------------|
| compileKotlin2Js               | [KotlinJsCompile] | A task is created for `main` source set |
| compileTestKotlin2Js           | [KotlinJsCompile] | A task is created for `test` source set |
| compile*SourceSetName*Kotlin2Js| [KotlinJsCompile] | A task is created for each additional source set |

Each [KotlinJsCompile] task provides `kotlinOptions` ([KotlinJsOptions]) extension:
```
compileKotlin2Js {
    kotlinOptions {
        noStdlib = true
    }
    // kotlinOptions.noStdlib = true
}
```