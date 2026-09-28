// ISSUE: KT-76529
// DUMP_IR
// FILE: IdContainer.java

import lombok.*;

@Getter
@Setter
@ToString
public class IdContainer {
	int id = 0;

    public int publicId = 1;
}

// FILE: main.kt

import kotlin.test.assertEquals

fun box(): String {
    val container = IdContainer()
    // Should call the setter/getter, and not access the field
    container.id = 123
    val i = container.id
    container.publicId = container.id
    container.id = container.publicId
    val s = container.toString()

    assertEquals(123, i)
    assertEquals("IdContainer(id=123, publicId=123)", s)

    return "OK"
}

// FILE: lombok.config

config.stopBubbling = true
lombok.addLombokGeneratedAnnotation = true
lombok.fieldDefaults.defaultPrivate = true
