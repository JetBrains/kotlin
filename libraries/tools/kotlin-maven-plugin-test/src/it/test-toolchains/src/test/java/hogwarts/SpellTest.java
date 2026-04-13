package hogwarts;

import org.junit.Test;
import static org.junit.Assert.*;

public class SpellTest {
    @Test
    public void testSpellCastByWizard() {
        Wizard hermione = new Wizard("Hermione Granger", "Gryffindor");
        Spell spell = new Spell("Wingardium Leviosa", 3);
        assertEquals("Hermione Granger casts Wingardium Leviosa!", spell.castBy(hermione));
    }
}
