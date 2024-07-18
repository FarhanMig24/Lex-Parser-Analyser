import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Token_Extractor {

    // Define regular expression patterns for the tokens
    private static final List<List<String>> REGEX_MAPPING = List.of(
        List.of("operator", "(==|=|<|>|\\+|-|\\*|\\?|\\:|\\^)"),
        List.of("punctuator", "(\\(|\\)|\\{|\\}|\\,|\\[|\\])"),
        List.of("delimiter", "(;)"),
        List.of("constant", "(\\d+|\'.\'|\".*\")"),
        List.of("keyword", "(get|put|return|int|char|string|main|for|if|else)"),
        List.of("identifier", "[a-zA-Z_]\\w*")
    );

    // Regex for whitespace
    public static Pattern whitespacePattern = Pattern.compile("\\s+");

    // Define a function to tokenize the input string
    public static ArrayList<Token> tokenize(String inputString) throws Exception {
        ArrayList<Token> tokens = new ArrayList<Token>();
        int position = 0;

        while (position < inputString.length()) {
            Matcher matcher = null;
            // update position if the current character is a whitespace
            matcher = whitespacePattern.matcher(inputString.substring(position));
            if (matcher.lookingAt()) {
                position += matcher.end();
                continue;
            }
            // Check for each token type
            for (List<String> tokenRegex : REGEX_MAPPING) {
                // compile the regex pattern for the token
                Pattern pattern = Pattern.compile(tokenRegex.get(1));
                matcher = pattern.matcher(inputString.substring(position));
                if (matcher.lookingAt()) {
                    // Get the line and column number of the token
                    int[] lineColumn = getLineAndColumn(inputString, position);

                    String text = matcher.group(0);
                    boolean charPatternMatched = false;
                    boolean stringPatternMatched = false;
                    boolean integerPatternMatched = false;

                    // For string and char tokens, remove the quotes
                    if (tokenRegex.get(0).equals("constant")) {
                        // check if constant is a string or char
                        Pattern charPattern = Pattern.compile("\'.\'");
                        Pattern stringPattern = Pattern.compile("\".*\"");
                        Matcher charPatternMatcher = charPattern.matcher(text);
                        Matcher stringPatternMatcher = stringPattern.matcher(text);
                        charPatternMatched = charPatternMatcher.matches();
                        stringPatternMatched = stringPatternMatcher.matches();
                        integerPatternMatched = !charPatternMatched && !stringPatternMatched;
                    }

                    if(stringPatternMatched){
                        tokens.add(new Token("constant", "string_constant", lineColumn[0], lineColumn[1]+1, matcher.end(), text));
                    }else if(charPatternMatched){
                        tokens.add(new Token("constant", "char_constant", lineColumn[0], lineColumn[1]+1, matcher.end(), text));
                    }else if(integerPatternMatched){
                        tokens.add(new Token("constant", "integer_constant", lineColumn[0], lineColumn[1]+1, matcher.end(), text));
                    }else if(tokenRegex.get(0).equals("identifier")){
                        tokens.add(new Token(tokenRegex.get(0), "id", lineColumn[0], lineColumn[1]+1, matcher.end(), text));
                    }else {
                        tokens.add(new Token(tokenRegex.get(0), text, lineColumn[0], lineColumn[1]+1, matcher.end()));
                    }

                    // Update the position
                    position += matcher.end();
                    break;
                }
            }
            // If no token matched, throw an exception
            if (matcher == null || !matcher.lookingAt()) {
                int[] lineColumn = getLineAndColumn(inputString, position);
                throw new Exception("Invalid token at line " + lineColumn[0] + " and column " + lineColumn[1]);
            }
        }
        return tokens;
    }

    public static int[] getLineAndColumn(String inputString, int position) {
        // 1-indexed
        // line no -> no of new lines before the position + 1
        // column no -> position - index of last new line
        int[] lineColumn = new int[2];
        int line = inputString.substring(0, position).split("\r?\n").length;
        int lastNewlinePos = inputString.substring(0, position).lastIndexOf('\n');
        int column = position - lastNewlinePos;
        lineColumn[0] = line;
        lineColumn[1] = column;
        return lineColumn;
    }

    public static String centerText(String input, int width) {
        if (input.length() >= width) {
            return input; // if the input is already wider than the desired width, return it as-is
        }
        
        int padding = width - input.length();
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < leftPadding; i++) {
            builder.append(" "); // add left padding
        }
        builder.append(input); // add the input text
        for (int i = 0; i < rightPadding; i++) {
            builder.append(" "); // add right padding
        }
        
        return builder.toString();
    }
    
    public static void displayTokens(ArrayList<Token> tokens){
        System.out.println(centerText("TYPE", 20) + centerText("TEXT", 20) + centerText("LINE", 10) + centerText("COLUMN", 10) + centerText("SIZE", 10) + centerText("VALUE", 20));
        for (Token token : tokens) {
            System.out.println(centerText(token.token_type, 20) + centerText(token.text, 20) + centerText(String.valueOf(token.line), 10) + centerText(String.valueOf(token.column), 10) + centerText(String.valueOf(token.size), 10) + centerText(String.valueOf(token.value), 20));
        }
    }
}
