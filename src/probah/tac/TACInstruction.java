package probah.tac;

public class TACInstruction {

    private final String text;

    public TACInstruction(String text) {

        this.text = text;
    }

    public String getText() {

        return text;
    }

    @Override
    public String toString() {

        return text;
    }
}