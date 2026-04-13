package hogwarts;

public class Spell {
    private final String incantation;
    private final int powerLevel;

    public Spell(String incantation, int powerLevel) {
        this.incantation = incantation;
        this.powerLevel = powerLevel;
    }

    public String getIncantation() {
        return incantation;
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public String castBy(Wizard wizard) {
        return wizard.getName() + " casts " + incantation + "!";
    }
}
