package ga.pmc.auskip;

public class Mana {
    private int value;

    public Mana(int value) {
        this.value = value;
    }

    public Mana plus(Mana other) {
        return new Mana(this.value + other.value);
    }

    public Mana minus(Mana other) {
        return new Mana(this.value - other.value);
    }

    public boolean isLessThan(Mana other) {
        return this.value < other.value;
    }

    public boolean isGreaterThan(Mana other) {
        return this.value > other.value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getCurrentMana() {
        return this.value;
    }

    public void setCurrentMana(int value) {
        this.value = value;
    }

    public void subtract(int value) {
        this.value -= value;
    }
}
