// FILE: Vehicle.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Vehicle {
    private String make;
    private String model;
}

// FILE: Car.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Car extends Vehicle {
    private int numberOfDoors;
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val carBuilder = Car.builder()
        .numberOfDoors(4)
        .make("Fiesta")
        .model("Ford")

    val car = carBuilder.build()
    assertEquals(4, car.numberOfDoors)
    assertEquals("Fiesta", car.make)
    assertEquals("Ford", car.model)

    return "OK"
}
