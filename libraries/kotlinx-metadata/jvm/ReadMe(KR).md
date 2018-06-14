# kotlinx-metadata-jvm
이 라이브러리는 Kotlin/JVM 컴파일러에 의해 생성 된 바이너리 파일의 메타 데이터, 즉 `.class` 및 `.kotlin_module` 파일을 읽고 수정하는 API를 제공합니다.

## 개요

`.class` 파일의 Kotlin 메타 데이타를 읽는 엔트리 포인트는 [`KotlinClassMetadata.read`](src/kotlinx/metadata/jvm/KotlinClassMetadata.kt)입니다. 이것이 취하는 데이터는 기본적으로 Kotlin 컴파일러에 의해 생성 된 클래스 파일 [`kotlin.Metadata`](../../stdlib/jvm/runtime/kotlin/Metadata.kt)에 쓰여진 [`KotlinClassHeader`](src/kotlinx/metadata/jvm/KotlinClassHeader.kt)에 캡슐화됩니다. `kotlinClassHeader`를 `kotlin.Metadata`에서 다시 읽거나 다른 리소스에서 읽은 다음 `KotlinClassMetadata.read`를 사용하여 클래스 메타 데이터의 올바른 인스턴스를 얻습니다.
(Kotlin 1.3버전까지는 값을 `kotlin.Metadata`에서 다시 읽는 것이 *Java sources*에서만 가능합니다. 이에 대한 참고 사항은 표준 라이브러리 내부에 있습니다. [KT-23602](https://youtrack.jetbrains.com/issue/KT-23602)를 참조하세요.)

```kotlin
val header = KotlinClassHeader(
    ...
    /* pass Metadata.k, Metadata.d1, Metadata.d2, etc as arguments ... */
)
val metadata = KotlinClassMetadata.read(header)
```

`KotlinClassMetadata`는 sealed 클래스로, Kotlin 컴파일러에 의해 생성 된 모든 종류의 클래스를 나타내는 하위 클래스가 있습니다. 당신이 특정 종류의 클래스를 읽고 간단한 수행을 해낼 수 있다고 확신하지 않는 한, `when`은 모든 가능성을 처리하는 좋은 선택입니다:

```kotlin
when (metadata) {
    is KotlinClassMetadata.Class -> ...
    is KotlinClassMetadata.FileFacade -> ...
    is KotlinClassMetadata.SyntheticClass -> ...
    is KotlinClassMetadata.MultiFileClassFacade -> ...
    is KotlinClassMetadata.MultiFileClassPart -> ...
    is KotlinClassMetadata.Unknown -> ...
}
```

우리가 `KotlinClassMetadata.Class`의 인스턴스를 얻었다고 가정해 봅시다; 일부 클래스가 조금 다른 형식의 메타 데이터를 가지는 것을 제외하면, 다른 종류의 클래스들은 모두 비슷하게 처리됩니다. 기본 메타 데이터를 이해하는 주요 방법은 'accept'를 호출하고 들어오는 정보를 처리하기 위해 [`KmClassVisitor`](../src/kotlinx/metadata/visitors.kt)의 인스턴스를 전달하는 것입니다(`Km`은 "Kotlin 메타 데이터"의 줄임말 임).

```kotlin
metadata.accept(object : KmClassVisitor() {
    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
        // This will be called for each function in the class. "name" is the
        // function name, and "flags" represent modifier flags (see below)

        ...

        // Return an instance of KmFunctionVisitor for more details,
        // or null if this function is of no interest
    }
}
```

클래스 메타 데이터에서 모든 인라인 함수를 JVM signiture과 함께 읽을 수 있게 한 예제는 [`MetadataSmokeTest.listInlineFunctions`](test/kotlinx/metadata/test/MetadataSmokeTest.kt)를 참조하십시오.

## Flags

수많은 `visit *` 메써드들은 `flags`라는 매개 변수를 취합니다. 이 플래그는 선언이나 데이터 형식의 boolean 속성을 나타냅니다. 특정 플래그가 있는지 확인하려면 주어진 정수 값에 대해 [`Flag`](../src/kotlinx/metadata/Flag.kt)에 있는 플래그 중 하나를 호출하십시오. 적용 가능한 플래그 세트는 각`visit*` 메소드에 문서화되어 있습니다. 예를 들어, 함수의 경우 이것은 일반적인 선언 플래그(가시성, 양식)와 `Flag.Function` 플래그를 더한 것입니다:

```kotlin
override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
    if (Flag.IS_PUBLIC(flags)) {
        println("function $name is public")
    }
    if (Flag.Function.IS_SUSPEND(flags)) {
        println("function $name has the 'suspend' modifier")
    }
    ...
}
```

## 확장

Kotlin/JS의 `.meta.js` 파일에 있는 특정 정보가 JS 전용인 것처럼, Kotlin  `.class` 파일의 메타 데이터에있는 특정 정보는 JVM 전용입니다. 우리는 메타데이터 라이브러리를 읽는 Kotlin/JS- (이후 Kotlin/Native-)를 공개할 가능성을 유지하기 위해 이 라이브러리의 API 대부분을 플랫폼 독립적인 [`kotlinx-metadata`](../)에 수록해놨습니다. (여기서 플랫폼 독립적이란 것은 컴파일을 하는 플랫폼에 제약을 받지 않는다는 것뿐 아니라 Kotlin 메타 데이터를 읽을 수있는 플랫폼도 제한이 없다는 것입니다.) 그리고 `kotlinx-metadata-jvm`은 JVM 전용 데이터가있는 작은 추가 기능입니다.

플랫폼 특정 (이 경우에는 JVM 특정) 데이터를 읽으려면 해당 데이터를 갖고있는 각 방문자가 [*확장 유형*](../src/kotlinx/metadata/extensions.kt)을 취하고 플랫폼 고유 데이터를 읽을 수 있는 타입의 방문자를 반환하는 `visitExtensions` 메소드를 선언합니다. JVM을 위한 `visitExtensions`을 구현하는 의도된 방법은 주어진 확장 타입이 필요한 JVM 확장 방문자의 것인지를 검사하고 그 방문객의 새로운 인스턴스를 반환하거나, 그렇지 않으면 null을 반환하는 것입니다. 각 JVM 확장 방문객은 동반자 객체의 `TYPE` 변수에 선언된 타입을 가집니다. 예를 들어, 사유 정보에서 JVM 확장을 읽으려면 다음을 입력하십시오:

```kotlin
override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
    // If these are JVM property extensions, read them by returning a visitor
    if (type == JvmPropertyExtensionVisitor.TYPE) {
        return object : JvmPropertyExtensionVisitor() {
            // Read JVM property extensions
            ...
        }
    }
    
    // If these are extensions of some other type, ignore them
    return null
}
```

## 메타데이터 쓰기

Kotlin 클래스 파일의 메타 데이터를 처음부터 만들려면 `KotlinClassMetadata` 하위 클래스에서 선언 된 `Writer` 클래스 중 하나를 사용하십시오. 관련 클래스의 저자는 해당 `Km*Visitor` 클래스에서 상속받습니다. 저자에게 `visit *` 메소드를 연속적으로 호출하여 선언문을 추가하고 (필요한 경우 visitEnd를 호출하는 것을 잊지 마십시오!), 마지막에는 `write`를 호출하여 메타 데이터를 생성하십시오. 그리고 `KotlinClassMetadata.header`를 사용하여 원시 데이터를 가져 와서 클래스 파일의 `kotlin.Metadata` 주석에 씁니다.

Kotlin 소스 코드의 메타 데이터 작성자를 사용하는 경우 상용구를 줄이기 위해 `run`과 같은 Kotlin 범위 지정 기능을 사용하는 것이 매우 편리합니다:

```kotlin
// Writing metadata of a class
val header = KotlinClassMetadata.Class.Writer().run {
    // Visiting the name and the modifiers on the class.
    // Flags are constructed by invoking "flagsOf(...)"
    visit(flagsOf(Flag.IS_PUBLIC), "MyClass")
    
    // Adding one public primary constructor
    visitConstructor(flagsOf(Flag.IS_PUBLIC, Flag.Constructor.IS_PRIMARY))!!.run {
        // Visiting JVM signature (for example, to be used by kotlin-reflect)
        (visitExtensions(JvmConstructorExtensionVisitor.TYPE) as JvmConstructorExtensionVisitor).run {
            visit("<init>()V")
        }
        
        // Not forgetting to call visitEnd at the end of visit of the declaration
        visitEnd()
    }
    
    ...
    ...
    
    // Finally writing everything to arrays of bytes
    write().header
}

// Use header.kind, header.data1, header.data2, etc. to write values to kotlin.Metadata
...
```

간단한 Kotlin 클래스의 메타 데이터가 생성 된 예제는 [`MetadataSmokeTest.produceKotlinClassFile`](test/kotlinx/metadata/test/MetadataSmokeTest.kt)을 참조하십시오. 그러면 클래스 파일은 ASM으로 생성되고 Kotlin reflection에 의해 로드됩니다.

## 모듈 메타 데이터

`KotlinClassMetadata`가 Kotlin`.class` 파일의 메타 데이타를 읽고 쓰는 방법과 유사하게 [`KotlinModuleMetadata`](src/kotlinx/metadata/jvm/KotlinModuleMetadata.kt)는 `.kotlin_module`파일을 읽고 쓰는 엔트리 포인트입니다. `KotlinModuleMetadata.read` 또는 `KotlinModuleMetadata.Writer`를 클래스 파일과 같은 방식으로 사용하십시오. 유일한 차이점은 읽기 전용 소스 (및 작성자 결과)는 `kotlin.Metadata`에서 로드된 구조화 된 데이터가 아니라 간단한 바이트 배열이라는 것입니다:

```kotlin
// Read the module metadata
val bytes = File("META-INF/main.kotlin_module").readBytes()
val metadata = KotlinModuleMetadata.read(bytes)
metadata.accept(object : KmModuleVisitor() {
    ...
}

// Write the module metadata
val bytes = KotlinModuleMetadata.Writer().run {
    visitPackageParts(...)
    
    write().bytes
}
File("META-INF/main.kotlin_module").writeBytes(bytes)
```

## Laziness

`KotlinClassMetadata` 또는 `KotlinModuleMetadata` 인스턴스에서 `accept`를 호출할 때까지 데이터는 완전히 파싱되어 검증되지 않습니다. 계속하기 전에 데이터가 끔찍하게 손상되지 않았는지 확인해야하는 경우 빈 방문자라도 'accept'를 호출해야합니다.

```kotlin
val metadata: KotlinClassMetadata.Class = ...

try {
    // Guarantees eager parsing of the underlying data
    metadata.accept(object : KmClassVisitor() {})
} catch (e: Exception) {
    System.err.println("Metadata is corrupted!")
}
```


