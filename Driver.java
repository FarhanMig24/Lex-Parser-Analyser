import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Driver {
    public static void main(String[] args) throws Exception{

        // Code input from code.txt
        Path fileName = Path.of("./Source_Code.txt");
        String code = Files.readString(fileName);
        System.out.println("\n\n");
        System.out.println("Code taken into consideration:");
        System.out.println(code); 

        // Generate tokens from the code
        ArrayList<Token> tokens = Token_Extractor.tokenize(code);
        System.out.println("\n-------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        // Display tokens
        System.out.println("\n\n");
        System.out.println("\n> Tokens: ");
        Token_Extractor.displayTokens(tokens);
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        System.out.println("\n\n");

        // Generate symbol table
        SymbolTable symbolTable = SymbolTable.generateFromTokens(tokens);
        symbolTable.display();
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        
        // Create instance of SLR
        Parser slr = new Parser();

        // Read production rules
        fileName = Path.of("./Grammer.txt");
        slr.readProductions(fileName);
        System.out.println("\n\n");
        System.out.println("> Production Rules : \n");
        // Display production rules
        slr.displayProductionRules();
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        System.out.println();

        // Set start symbol
        Parser.START_SYMBOL = "prog";

        // Generate first/follow set
        slr.computeFirstPos();
        System.out.println("\n\n");
        // Display first/follow set
        System.out.println("> First / Follow Set \n");
        slr.displayFirstAndFollowPosTable();
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
       
        // Generate LR(0) Item Set
        slr.generateItemSets();
        // Display LR(0) Item Set
        System.out.println("\n\n");
        System.out.println("> LR(0) Item Sets : \n");
        slr.displayItemSets();

        // // Display goto table
        System.out.println("\n\n");
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        System.out.println("\n> Goto Table : \n");
        slr.displayGotoTable("Goto_Action.txt");
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        
        // Display production numbers
        System.out.println("\n\n");
        System.out.println("\n> Production Numbers : \n");
        slr.displayNumberedProductionRules();
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        
        // Generate SLR Parsing Table
        slr.generateParsingTable();
        // Display SLR Parsing Table
        System.out.println("\n\n");
        System.out.println("\n> SLR Parsing Table : \n");
        slr.displayParsingTable("Parsing_Table.txt");
        System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        
        // Parse tokens
       StringBuilder output = new StringBuilder();
       for (Token token : tokens) {
           output.append(token.text);
           output.append(" ");
       }
       System.out.println("\n\n");
       System.out.println("\n> Parsing Tokens : \n");
       slr.parseInput(output.toString(),"Parsing_Token_Output.txt");
       System.out.println("\n---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
    }
}
