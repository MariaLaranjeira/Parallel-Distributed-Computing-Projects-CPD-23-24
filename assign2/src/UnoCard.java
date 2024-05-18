import java.util.Objects;

public class UnoCard {
    private String color;
    private String value;

    public UnoCard(String color, String value) {
        this.color = color;
        this.value = value;
    }

    public String getColor() {
        return color;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return color + "_" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnoCard unoCard = (UnoCard) o;
        return color.equals(unoCard.color) && value.equals(unoCard.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, value);
    }
}
