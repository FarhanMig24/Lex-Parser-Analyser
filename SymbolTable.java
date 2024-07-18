import java.util.*;

public class SymbolTable {
    
    private Map<String, List<Symbol>> symbols;
    private int currentScope;
    
    public SymbolTable() {
        symbols = new HashMap<>();
        currentScope = 0;
    }
    
    public void enterScope() {
        currentScope++;
    }
    
    public void exitScope() {
        if (currentScope == 0) {
            throw new RuntimeException("Cannot exit global scope");
        }
        currentScope--;
    }
    
    public void insert(String name, String type) {
        if (symbols.containsKey(name)) {
            List<Symbol> symbolList = symbols.get(name);
            // Check if symbol is already defined in current scope
            for (Symbol symbol : symbolList) {
                if (symbol.getScope() == currentScope) throw new RuntimeException("Symbol already defined in current scope: " + name);
            }
            symbolList.add(new Symbol(name, type, currentScope));
        } else {
            List<Symbol> symbolList = new ArrayList<>();
            symbolList.add(new Symbol(name, type, currentScope));
            symbols.put(name, symbolList);
        }
    }
    
    public void modify(String name, String newValue) {
        if (!symbols.containsKey(name)) throw new RuntimeException("Symbol not found: " + name);
        List<Symbol> symbolList = symbols.get(name);
        Symbol lastSymbol = symbolList.get(symbolList.size() - 1);
        if (lastSymbol.getScope() <= currentScope) {
            lastSymbol.setValue(newValue);
        } else {
            throw new RuntimeException("Cannot modify symbol in outer scope: " + name);
        }
    }
    
    public Symbol lookup(String name) {
        if (!symbols.containsKey(name)) return null;
        List<Symbol> symbolList = symbols.get(name);
        for (int i = symbolList.size()-1; i >=0 ; i--) {
            if (symbolList.get(i).getScope() <= currentScope) {
                return symbolList.get(i);
            }
        }
        return null;
    }

    public void display() {
        System.out.println("\n> Symbol Table:");
        for (String name : symbols.keySet()) {
            List<Symbol> symbolList = symbols.get(name);
            for (Symbol symbol : symbolList) {
                System.out.println(symbol.getName() + " = ( type : "+symbol.getType()+", value : " + symbol.getValue() + ", scope: " + symbol.getScope() + ")");
            }
        }
        System.out.println("\n\n");
    }

    public static SymbolTable generateFromTokens(List<Token> tokens){
        Token lastToken = null;
        ArrayList<String> identifierTypes = new ArrayList<>();
        identifierTypes.add("int");
        identifierTypes.add("char");
        identifierTypes.add("string");

        SymbolTable symbolTable = new SymbolTable();

        for (Token token : tokens) {
            if(token.token_type.equals("identifier")){
                if(lastToken == null || !lastToken.token_type.equals("keyword") || !identifierTypes.contains(lastToken.text)){

                }
                else{
                    symbolTable.insert(token.value, lastToken.text);
                }
            }
            else if(token.token_type.equals("punctuator")){
                if(token.text.equals("{")) symbolTable.enterScope();
                else if(token.text.equals("}")) symbolTable.exitScope();
            }

            lastToken = token;
        }
        return symbolTable;
    }
    
    private class Symbol {
        private String name;
        private String value;
        private String type;
        private int scope;
        
        public Symbol(String name, String type, int scope) {
            this.name = name;
            this.type = type;
            this.scope = scope;
            if(type.equals("int")){
                this.value = "0";
            }else if(type.equals("char") || type.equals("string")){
                this.value = "";
            }else{
                this.value = null;
            }
        }
        
        public String getName() {
            return name;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public int getScope() {
            return scope;
        }   

        public String getType() {
            return type;
        }
    }
    
}
