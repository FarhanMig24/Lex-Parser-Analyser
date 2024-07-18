public class Token {
    public String token_type;
    public String text;
    public int line;
    public int column;
    public int size;
    public String value;

    public Token(String type, String text, int line, int column, int size) {
        this.token_type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.size = size;
        this.value = null;
    }

    public Token(String type, String text, int line, int column, int size, String value) {
        this.token_type = type;
        this.text = text;
        this.line = line;
        this.column = column;
        this.size = size;
        this.value = value;
    }


    @Override
    public String toString() {
        return String.format("(%s, %s, %d, %d, %d, %s)", token_type, text, line, column, size, value);
    }
}