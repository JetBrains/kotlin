# Kotlin 공용 API 바이너리 호환성 확인 도구

이 도구는 Kotlin 라이브러리의 바이너리 API를 dump 할 수 있게 해줍니다.
그리고 공용 바이너리 API가 호환되지 않는 방식으로 변경되었는지는 아닌지 확인합니다.

## 실행 방법

컴파일 후 테스트들을 실행합니다. `CasesPublicAPITest` 는 도구 자체를 확인합니다, 
그리고 `RuntimePublicAPITest`가 `kotlin-stdlib`의 공용 API를 dump합니다, 
incremental 컴파일은 현재 모든 요구되는 출력을 만들어 내지 않기 때문에,
`kotlin-stdlib-jdk7/8`, `kotlin-stdlib-jre7/8` 그리고 `kotlin-reflect` jar,
는 사전에 반드시 gradle로 작성되어야 합니다. `clean assemble` task들을 사용하십시요.

공용 API를 크게 변경했을때, 이것은 덮어쓰기를 하는 것을 도와줍니다.
전체 dump와 변경사항을 commit하기 전에 비교하십시요 : `-Doverwrite.output=true`를 pass하십시요 
property를 테스트할 수 있습니다.

또한 공유된 "Binary compatibility tests"를 사용하여 실행할 수 있습니다. 이것은 다른 결과를 덮어씌웁니다.

## What constitutes the public API

### Classes

A class is considered to be effectively public if all of the following conditions are met:

 - it has public or protected JVM access (`ACC_PUBLIC` or `ACC_PROTECTED`)
 - it has one of the following visibilities in Kotlin:
    - no visibility (means no Kotlin declaration corresponds to this compiled class)
    - *public*
    - *protected*
    - *internal*, only in case if the class is annotated with `InlineExposed`
 - it isn't a local class
 - it isn't a synthetic class with mappings for `when` tableswitches (`$WhenMappings`)
 - it contains at least one effectively public member, in case if the class corresponds
   to a kotlin *file* with top-level members or a *multifile facade*
 - in case if the class is a member in another class, it is contained in the *effectively public* class
 - in case if the class is a protected member in another class, it is contained in the *non-final* class

### Members

A member of the class (i.e. a field or a method) is considered to be effectively public
if all of the following conditions are met:

 - it has public or protected JVM access (`ACC_PUBLIC` or `ACC_PROTECTED`)
 - it has one of the following visibilities in Kotlin:
    - no visibility (means no Kotlin declaration corresponds to this class member)
    - *public*
    - *protected*
    - *internal*, only in case if the class is annotated with `InlineExposed`

    > Note that Kotlin visibility of a field exposed by `lateinit` property is the visibility of it's setter.
 - in case if the member is protected, it is contained in *non-final* class
 - it isn't a synthetic access method for a private field

## What makes an incompatible change to the public binary API

### Class changes

For a class a binary incompatible change is:

 - changing the full class name (including package and containing classes)
 - changing the superclass, so that the class no longer has the previous superclass in
   the inheritance chain
 - changing the set of implemented interfaces so that the class
   no longer implements interfaces it had implemented before
 - changing one of the following access flags:
    - `ACC_PUBLIC`, `ACC_PROTECTED`, `ACC_PRIVATE` — lessening the class visibility
    - `ACC_FINAL` — making non-final class final
    - `ACC_ABSTRACT` — making non-abstract class abstract
    - `ACC_INTERFACE` — changing class to interface and vice versa
    - `ACC_ANNOTATION` — changing annotation to interface and vice versa

### Class member changes

For a class member a binary incompatible change is:

 - changing its name
 - changing its descriptor (erased return type and parameter types for methods);
   this includes changing field to method and vice versa
 - changing one of the following access flags:
    - `ACC_PUBLIC`, `ACC_PROTECTED`, `ACC_PRIVATE` — lessening the member visibility
    - `ACC_FINAL` — making non-final field or method final
    - `ACC_ABSTRACT` — making non-abstract method abstract
    - `ACC_STATIC` — changing instance member to static and vice versa
