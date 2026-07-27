// IGNORE_BACKEND: ANY

// ISSUE: KT-87889

// FILE: Singularization.java

import lombok.Builder;
import lombok.Singular;
import java.util.List;

@Builder
class Singularization {
    @Singular
    public List<Integer> quizzes;

    @Singular
    public List<Integer> matrices;

    @Singular
    public List<Integer> indices;

    @Singular
    public List<Integer> vertices;

    @Singular
    public List<Integer> statuses;

    @Singular
    public List<Integer> aliases;

    @Singular
    public List<Integer> pickaxes;

    @Singular
    public List<Integer> sexes;

    @Singular
    public List<Integer> testes;

    @Singular
    public List<Integer> movies;

    @Singular
    public List<Integer> octopodes;

    @Singular
    public List<Integer> buses;

    @Singular
    public List<Integer> mice;

    @Singular
    public List<Integer> lice;

    @Singular
    public List<Integer> men;

    @Singular
    public List<Integer> women;

    @Singular
    public List<Integer> minutiae;

    @Singular
    public List<Integer> shoes;

    @Singular
    public List<Integer> synopses;

    @Singular
    public List<Integer> prognoses;

    @Singular
    public List<Integer> theses;

    @Singular
    public List<Integer> diagnoses;

    @Singular
    public List<Integer> bases;

    @Singular
    public List<Integer> analyses;

    @Singular
    public List<Integer> crises;

    @Singular
    public List<Integer> children;

    @Singular
    public List<Integer> moves;

    @Singular
    public List<Integer> zombies;

    @Singular
    public List<Integer> colloquies;

    @Singular
    public List<Integer> babies;

    @Singular
    public List<Integer> tomatoes;

    @Singular
    public List<Integer> hives;

    @Singular
    public List<Integer> alternatives;

    @Singular
    public List<Integer> bosses;

    @Singular
    public List<Integer> matches;

    @Singular
    public List<Integer> boxes;

    @Singular
    public List<Integer> dishes;

    @Singular
    public List<Integer> wolves;

    @Singular
    public List<Integer> scarves;

    @Singular
    public List<Integer> saves;

    @Singular
    public List<Integer> leaves;

    @Singular
    public List<Integer> objects;
}

// FILE: test.kt

fun box(): String {
    Singularization.builder().apply {
        quiz(0)
        matrix(1)
        index(2)
        vertex(3)
        status(4)
        alias(5)
        pickaxe(6)
        sex(7)
        testis(8)
        movie(9)
        <!UNRESOLVED_REFERENCE!>octopus<!>(10)
        bus(11)
        mouse(12)
        louse(13)
        man(14)
        woman(15)
        <!UNRESOLVED_REFERENCE!>minutia<!>(16)
        shoe(17)
        synopsis(18)
        prognosis(19)
        thesis(20)
        diagnosis(21)
        <!UNRESOLVED_REFERENCE!>base<!>(22)
        analysis(23)
        crisis(24)
        child(25)
        move(26)
        zombie(27)
        colloquy(28)
        baby(29)
        tomato(30)
        hive(31)
        alternative(32)
        boss(33)
        match(34)
        box(35)
        dish(36)
        wolf(37)
        scarf(38)
        save(39)
        leaf(40)
        `object`(41)
    }

    return "OK"
}
