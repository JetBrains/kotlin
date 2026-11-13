// One hierarchy using `@SuperBuilder` from both `lombok.experimental` and the main `lombok` package (since Lombok 1.18.48)

// FILE: Vehicle.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Vehicle {
    private String make;
}

// FILE: Car.java

import lombok.Data;
import lombok.SuperBuilder;

@SuperBuilder
@Data
public class Car extends Vehicle {
    private int numberOfDoors;
}

// FILE: SportsCar.java

import lombok.Data;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class SportsCar extends Car {
    private int topSpeed;
}

// FILE: test.kt

import kotlin.test.assertEquals

fun box(): String {
    val car = Car.builder()
        .numberOfDoors(4)
        .make("Ford")
        .build()
    assertEquals(4, car.numberOfDoors)
    assertEquals("Ford", car.make)

    val sportsCar = SportsCar.builder()
        .topSpeed(300)
        .numberOfDoors(2)
        .make("Ferrari")
        .build()
    assertEquals(300, sportsCar.topSpeed)
    assertEquals(2, sportsCar.numberOfDoors)
    assertEquals("Ferrari", sportsCar.make)

    return "OK"
}
