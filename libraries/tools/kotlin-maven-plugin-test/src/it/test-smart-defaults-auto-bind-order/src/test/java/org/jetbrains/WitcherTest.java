package org.jetbrains;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WitcherTest {

    @Test
    public void geraltCastsAard() {
        Witcher geralt = WitcherSchool.createWhiteWolf();
        assertEquals("Geralt casts AARD, a telekinetic sign", geralt.cast());
    }

    @Test
    public void bestiaryKnowsWraithWeakness() {
        Bestiary bestiary = new Bestiary();
        assertEquals(Sign.YRDEN, bestiary.monsterWeakness("wraith"));
    }
}
