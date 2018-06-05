## kotlinx-metadata

여기는 Kotlin Metadata 라이브러리의 플랫폼 독립적 부분이며, Kotlin 컴파일러로부터 생성된 binary (`.class`, `.js`)파일에 있는 Kotlin 선언(declaration)의 metadata를 읽고 수정하는 것이 가능하게끔 하기 위한 의도를 가지고 있습니다. 

라이브러리의 이 부분은 현재 독립적으로 배포(release)되고 있지 않습니다.

[kotlinx-metadata-jvm](jvm/ReadMe.md)에서 JVM 바이너리 (`.class` 와 `.kotlin_module` 파일)의 Kotlin Metadata를 읽어오고 수정하는 방법에 대한 지시사항(instructions)을 확인할 수 있습니다.