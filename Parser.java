import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Parser {
    private List<String> nonTerminalKeys;
    private Map<String, List<List<String>>> production_rules;
    private Map<String, List<String>> production_rules_numbered; // only for parsing purpose, to lookup reduced production
    public Map<String, Set<String>> firstPos;
    public Map<String, Set<String>> followPos;
    public static final String EPSILON = "EPSILON";
    public static final String DOLLAR = "$";
    public static String START_SYMBOL = null;
    private Map<Integer, Map<String, List<List<String>>>> itemSets;
    private Map<Integer, Map<String, Integer>> gotoTable;
    private Map<Integer, Map<String, String>> parsingTable ;

    public Parser() {
        nonTerminalKeys = new ArrayList<>();
        production_rules = new HashMap<>();
        production_rules_numbered = new HashMap<>();
        firstPos = new HashMap<>();
        followPos = new HashMap<>();
        itemSets = new HashMap<>();
        gotoTable = new HashMap<>();
        parsingTable = new HashMap<>();
    }

    public void readProductions(Path path) throws Exception {
        List<String> raw_lines = Files.readAllLines(path);
        for(String line : raw_lines) {
            String[] parts = line.split("->");
            String nonTerminal = parts[0].trim();
            nonTerminalKeys.add(nonTerminal);
            List<List<String>> productions = new ArrayList<>();
            String[] production_parts = parts[1].trim().split("\\|");
            for (String production_part : production_parts) {
                String[] symbols = production_part.trim().split(" ");
                List<String> tmp = Arrays.asList(symbols).stream()
                                    .map(String::trim)
                                    .filter(x -> !x.isEmpty())
                                    .collect(Collectors.toList());
                productions.add(tmp);
            }
            production_rules.put(nonTerminal, productions);
        }

        // generate the numbered productions
        for(String symbol : production_rules.keySet()) {
            List<List<String>> productions = production_rules.get(symbol);
            for (int i = 0; i < productions.size(); i++) {
                List<String> production = productions.get(i);
                if(production.size() == 1 && production.get(0).equals(EPSILON)){
                    production = new ArrayList<>();
                }
                production_rules_numbered.put(symbol+"_"+i, production);
            }
        }
    }

    public boolean isNonTerminal(String symbol) {
        return production_rules.containsKey(symbol);
    }

    public boolean isTerminal(String symbol) {
        return !isNonTerminal(symbol);
    }

    public Set<String> findFirst(String symbol) {
        /**
         * Algorithm:
         * 1. If symbol is a terminal, return symbol
         * 2. If symbol is epsilon, return epsilon
         * 3. If already computed, return firstPos
         * 4. Fetch productions of symbol
         * 5. For each production, find first of first symbol
         * 6. Store result in firstPos
         */
        Set<String> first = new HashSet<>();
        // 1
        if(!isNonTerminal(symbol)) {
            first.add(symbol);
            return first;
        }
        // 2
        if(isNullable(symbol)) {
            first.add(EPSILON);
            return first;
        }
        // if(firstPos.containsKey(symbol)) {
        //     return firstPos.get(symbol);
        // }
        // 4
        List<List<String>> productions = production_rules.get(symbol);
        // 5
        for (List<String> production : productions) {            
            for (int i = 0; i < production.size(); i++) {
                Set<String> firstSymbolFirst = findFirst(production.get(i));
                if(isNullable(production.get(i)) || !firstSymbolFirst.contains(EPSILON)) {
                    first.addAll(firstSymbolFirst);
                    break;
                }else{
                    // remove only if it is not the last symbol
                    if(i + 1 < production.size()) {
                        firstSymbolFirst.remove(EPSILON);
                    }
                    first.addAll(firstSymbolFirst);
                }
            }
        }
        // 6
        firstPos.put(symbol, new HashSet<>(first));
        return first;
    }

    public Set<String> findFollow(String symbol) {
        /*
         * Algorithm:
         * 1. Fetch all productions
         * 2. If symbol is start symbol, add $ to follow list
         * 3. For each production parts, find follow of symbol
         * 4. If symbol is last character, add follow(start_symbol) to follow
         * 5. If character next to symbol is terminal, add it to follow
         * 6. If character next to symbol is non-terminal, add first(character) to follow
         */
        if(followPos.containsKey(symbol)) {
            return followPos.get(symbol);
        }
        HashSet<String> follow = new HashSet<>();
        if(isNullable(symbol)) {
            return follow;
        }

        if(START_SYMBOL.equals(symbol)) {
            follow.add(DOLLAR);
        }

        // 1 
        for (String nonTerminal : production_rules.keySet()) {
            // 3
            List<List<String>> productions = production_rules.get(nonTerminal);
            for (List<String> production : productions) {
                for (int i = 0; i < production.size(); i++) {
                    String currentSymbol = production.get(i);
                    String nextSymbol = i + 1 < production.size() ? production.get(i + 1) : null;
                    if(currentSymbol.equals(symbol)) {
                        // 4
                        if(nextSymbol == null) {
                            // last symbol
                            if(!nonTerminal.equals(symbol)){
                                follow.addAll(findFollow(nonTerminal));
                            }
                        }else{
                            // 5
                            if(!isNonTerminal(nextSymbol)) {
                                // terminal
                                follow.add(nextSymbol);
                            }else{
                                // 6
                                Set<String> first = findFirst(nextSymbol);
                                // if first has epsilon then go for next symbol in production
                                if(first.contains(EPSILON)){
                                    first.remove(EPSILON);
                                    follow.addAll(first);
                                    if(i + 2 < production.size()) {
                                        follow.addAll(findFirst(production.get(i + 2)));
                                    }else{
                                        follow.addAll(findFollow(nonTerminal));
                                    }
                                }else{
                                    follow.addAll(first);
                                }
                            }
                        }
                    }
                }
            }
        }

        followPos.put(symbol, follow);
        return follow;

    }

    public boolean isNullable(String symbol) {
        return symbol.equals(EPSILON);
    }

    public void computeFirstPos() {
        for (String nonTerminal : production_rules.keySet()) {
            findFirst(nonTerminal);
        }
    }
    
    public Map<String, List<List<String>>> closure(String symbol) {
        Map<String, List<List<String>>> closureSet = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        List<String> startRule = new ArrayList<>(Arrays.asList(".", symbol));
        List<List<String>> startClosure = new ArrayList<>(Arrays.asList(startRule));
        closureSet.put(symbol, startClosure);
        queue.offer(symbol);
        while (!queue.isEmpty()) {
            String nextSymbol = queue.poll();
            List<List<String>> nextClosureSet = closureSet.get(nextSymbol);
            for (int i = 0; i < nextClosureSet.size(); i++) {
                List<String> nextAugmentedRule = new ArrayList<>(nextClosureSet.get(i));
                if (nextAugmentedRule.indexOf(".") == nextAugmentedRule.size() - 1) {
                    continue;
                }
                String nextNextSymbol = nextAugmentedRule.get(nextAugmentedRule.indexOf(".") + 1);
                if (!production_rules.containsKey(nextNextSymbol)) {
                    continue;
                }
                List<List<String>> nextNextClosureSet = closureSet.getOrDefault(nextNextSymbol, new ArrayList<>());
                for (List<String> nextProductionRule : production_rules.get(nextNextSymbol)) {
                    List<String> nextNextAugmentedRule = new ArrayList<>(nextProductionRule);
                    nextNextAugmentedRule.add(0, ".");
                    boolean added = false;
                    for (Iterator<List<String>> it = nextNextClosureSet.iterator(); it.hasNext();) {
                        List<String> rule = it.next();
                        if (rule.equals(nextNextAugmentedRule)) {
                            added = true;
                            break;
                        }
                    }
                    if (!added) {
                        nextNextClosureSet.add(nextNextAugmentedRule);
                        closureSet.put(nextNextSymbol, nextNextClosureSet);
                        queue.offer(nextNextSymbol);
                    }
                }
            }
        }
        // delete the self E -> . E
        if(startClosure.size() > 0){
            closureSet.get(symbol).remove(startClosure.get(0));
        }
        // if E -> . EPSILON is there, delete EPSILON from first set
        for (String nonTerminal : closureSet.keySet()) {
            List<List<String>> rules = closureSet.get(nonTerminal);
            for (int i = 0; i < rules.size(); i++) {
                List<String> rule = rules.get(i);
                if(rule.size() == 2 && rule.get(1).equals(EPSILON)){
                    rule.remove(1);
                }
            }
            for (List<String> rule : rules) {
                if(rule.size() == 2 && rule.get(1).equals(EPSILON)){
                    firstPos.get(nonTerminal).remove(EPSILON);
                }
            }
        }

        return closureSet;
    }
    
    public void preapareFirstSet(){
        Map<String, List<List<String>>> closureSet = closure(START_SYMBOL);
        closureSet.put(START_SYMBOL+"'", new ArrayList<>(List.of(new ArrayList<>(Arrays.asList(".", START_SYMBOL)))));
        itemSets.put(itemSets.size(), closureSet);
    }

    public Map<String, List<List<String>>> gotoSet(Map<String, List<List<String>>> closureSet, String symbol) {
        Map<String, List<List<String>>> closureSetCopy = new HashMap<>();
        for (String nonTerminal : closureSet.keySet()) {
            List<List<String>> rules = new ArrayList<>();
            for (List<String> rule : closureSet.get(nonTerminal)) {
                rules.add(new ArrayList<>(rule));
            }
            closureSetCopy.put(nonTerminal, rules);
        }



        Map<String, List<List<String>>> gotoSets = new HashMap<>();
        // filter the closure which has symbol after dot
        for (String nonTerminal : closureSetCopy.keySet()) {
            List<List<String>> rules = new ArrayList<>();
            for (List<String> rule : closureSetCopy.get(nonTerminal)) {
                if (rule.indexOf(".") + 1 < rule.size() && rule.get(rule.indexOf(".") + 1).equals(symbol)) {
                    rules.add(rule);
                }
            }
            if(rules.size() > 0){
                gotoSets.put(nonTerminal, rules);
            }
        }

        // move the dot
        for (String nonTerminal : gotoSets.keySet()) {
            List<List<String>> rules = gotoSets.get(nonTerminal);
            for (List<String> rule : rules) {
                int dotIndex = rule.indexOf(".");
                rule.set(dotIndex, rule.get(dotIndex + 1));
                rule.set(dotIndex + 1, ".");
            }
        }

        // generate the list of symbols after dot
        Set<String> symbols_after_dot = new HashSet<>();
        for (String nonTerminal : gotoSets.keySet()) {
            List<List<String>> rules = gotoSets.get(nonTerminal);
            for (List<String> rule : rules) {
                int dotIndex = rule.indexOf(".");
                if(dotIndex + 1 < rule.size()){
                    symbols_after_dot.add(rule.get(dotIndex + 1));
                }
            }
        }

        // generate closure set for each symbol after dot
        for (String symbol_after_dot : symbols_after_dot) {
            Map<String, List<List<String>>> closureSetForSymbol = closure(symbol_after_dot);
            for (String nonTerminal : closureSetForSymbol.keySet()) {
                List<List<String>> rules = closureSetForSymbol.get(nonTerminal);
                if(rules.size() > 0){
                    if(gotoSets.containsKey(nonTerminal)){
                        gotoSets.get(nonTerminal).addAll(rules);
                    }else{
                        gotoSets.put(nonTerminal, rules);
                    }
                }
            }
        }

        return gotoSets;
    }

    public void generateItemSets() {
        // Prepare I0
        preapareFirstSet();
        // System.out.println("Item Sets > "+itemSets);
        // Create list of symbols
        HashSet<String> symbols = new HashSet<>();
        for (String nonTerminal : production_rules.keySet()) {
            symbols.add(nonTerminal);
            for (List<String> rule : production_rules.get(nonTerminal)) {
                symbols.addAll(rule);
            }
        }
        symbols.remove(EPSILON);
        // Generate item sets
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(0);

        while (!queue.isEmpty()) {
            int itemSetId = queue.poll();
            // System.out.println("Item Set Id > "+itemSetId);
            Map<String, List<List<String>>> itemSetMap = itemSets.get(itemSetId);
            
            for (String symbol : symbols) {
                Map<String, List<List<String>>> gotoSet = gotoSet(itemSetMap, symbol);
                // System.out.println("ID > "+itemSetId+" Symbol > "+symbol+" Goto Set > "+gotoSet);

                if (gotoSet.size() == 0) {
                    continue;
                }

                // check if there is already existing item set with sa me content as gotoSet
                boolean found = false;
                int movedState = -1;
                for (int i = 0; i < itemSets.size(); i++) {
                    if (itemSets.get(i).equals(gotoSet)) {
                        movedState = i;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    movedState = itemSets.size();
                    itemSets.put(itemSets.size(), gotoSet);
                    queue.offer(itemSets.size() - 1);
                }

                // entry in goto table
                if (gotoTable.containsKey(itemSetId)) {
                    gotoTable.get(itemSetId).put(symbol, movedState);
                } else {
                    gotoTable.put(itemSetId, new HashMap<>());
                    gotoTable.get(itemSetId).put(symbol, movedState);
                }

            }
        }
    }

    public void generateParsingTable(){
        // rows -> size of item sets
        // columns -> size of symbols
        // I0 -> { * -> S4, + -> ACC ,  * -> R5}
        parsingTable = new HashMap<>();
        HashSet<String> symbols = new HashSet<>();
        for (String nonTerminal : production_rules.keySet()) {
            symbols.add(nonTerminal);
            for (List<String> rule : production_rules.get(nonTerminal)) {
                symbols.addAll(rule);
            }
        }
        symbols.remove(EPSILON);
        Set<String> terminals = symbols.stream().filter(x -> !isNonTerminal(x)).collect(Collectors.toSet());
        // Set<String> nonTerminals = symbols.stream().filter(x -> isNonTerminal(x)).collect(Collectors.toSet());
        
        // create hashmaps
        for (int i = 0; i < itemSets.size(); i++) {
            parsingTable.put(i, new HashMap<>());
        }

        // fill the table with shift
        for (Integer startState : gotoTable.keySet()){
            for (String symbol : gotoTable.get(startState).keySet()){
                if(terminals.contains(symbol)){
                    parsingTable.get(startState).put(symbol, "S_"+gotoTable.get(startState).get(symbol));
                }else{
                    parsingTable.get(startState).put(symbol, gotoTable.get(startState).get(symbol).toString());
                }
            }
        }

        // fill the accept state
        for(Integer itemSetId : itemSets.keySet()){
            Map<String, List<List<String>>> itemSet = itemSets.get(itemSetId);
            for (String symbol : itemSet.keySet()) {
                if(symbol.equals(START_SYMBOL+"'")){
                    List<List<String>> rules = itemSet.get(symbol);
                    for (List<String> rule : rules) {
                        if(rule.get(rule.size() - 1).equals(".") && rule.get(0).equals(START_SYMBOL)){
                            parsingTable.get(itemSetId).put("$", "ACC");
                        }
                    }
                }
            }
        }

        // fill the reduce state
        for(Integer itemSetId : itemSets.keySet()){
            // itemset
            Map<String, List<List<String>>> itemSet = itemSets.get(itemSetId);
            // each rule in itemset
            for (String symbol : itemSet.keySet()) {
                // if non terminal
                if(isNonTerminal(symbol)){
                    List<List<String>> rules = itemSet.get(symbol);
                    for (List<String> rule : rules) {
                        if(rule.get(rule.size() - 1).equals(".")){
                            List<String> ruleCopy = new ArrayList<>(rule);
                            ruleCopy.remove(".");
                            // find ruleCopy in production rules numbered
                            for(String nonTerminal: production_rules_numbered.keySet()){
                                if(nonTerminal.startsWith(symbol+"_") && production_rules_numbered.get(nonTerminal).equals(ruleCopy)){
                                    // find follow
                                    Set<String> follows = findFollow(symbol);
                                    // fill in parsing table
                                    for (String follow : follows) {
                                        parsingTable.get(itemSetId).put(follow, "R_"+nonTerminal);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void parseInput(String input, String outputFile) {
        ArrayList<String> inputList = new ArrayList<>(Arrays.asList(input.split(" ")));
        inputList.add("$");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            // writer.write(inputList.toString());
            // writer.newLine();
            
            Stack<String> stateStack = new Stack<>();
            Queue<String> inputQueue = new LinkedList<>(inputList);
            stateStack.push("0");
            int ij=1;
            while (true) {
                String currentState = stateStack.peek();
                String currentInput = inputQueue.peek();
                if (isDigit(currentState)) {
                    Integer currentState__int = Integer.parseInt(currentState);
                    if (parsingTable.get((currentState__int)).containsKey(currentInput)) {
                        String action = parsingTable.get(currentState__int).get(currentInput);
                        if (action.startsWith("S")) {
                            // shift operation
                            stateStack.push(currentInput);
                            stateStack.push(action.split("_")[1]);
                            inputQueue.poll();
                        } else if (action.startsWith("R")) {
                            // reduce operation
                            String nonTerminal = action.split("_", 2)[1];
                            List<String> ruleList = production_rules_numbered.get(nonTerminal);
                            // pop the stack ruleLength*2 times
                            Integer ruleLength = ruleList.size();
                            for (int i = 0; i < ruleLength; i++) {
                                stateStack.pop();
                                stateStack.pop();
                            }
                            // add non-terminal to stack
                            String s = nonTerminal.split("_")[0];
                            stateStack.push(s);
    
                            // take last 2 elements of stack
                            String lastState = stateStack.get(stateStack.size() - 2);
                            String lastNonTerminal = stateStack.peek();
    
                            // find state from lastState, lastNonTerminal
                            String c = parsingTable.get(Integer.parseInt(lastState)).get(lastNonTerminal);
                            // push state to stack
                            stateStack.push(c);
                        } else if (action.equals("ACC")) {
                            writer.write("Accepted");
                            break;
                        } else {
                            writer.write("Parsing Failed !");
                            break;
                        }
                        writer.write(ij++ +":\tAction: " + action + "\n \tStack: " + stateStack + "\n \tInput Queue: " + inputQueue+"\n");
                        writer.newLine();
                    } else {
                        writer.write("Can't find action for state: " + currentState + " and input: " + currentInput + " in parsing table !");
                        writer.newLine();
                        writer.write("Parsing failed !");
                        break;
                    }
                } else {
                    writer.write("Parsing failed !");
                    writer.newLine();
                    writer.write("In stack, the last element is not a state !");
                    break;
                }
            }
        } 
        catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        System.out.println("Data written to " + outputFile + " successfully.");

    }

    public boolean isDigit(String s){
        return s.chars().allMatch(Character::isDigit);
    }

    // Display functions
    public void displayProductionRules(){
        for (String nonTerminal : production_rules.keySet()) {
            for (List<String> rule : production_rules.get(nonTerminal)) {
                System.out.print(nonTerminal+" -> ");
                for (String symbol : rule) {
                    System.out.print(symbol+" ");
                }
                System.out.println();
            }
        }
    }

    public void displayParsingTable(String filename) {
        HashSet<String> symbols = new HashSet<>();
        for (String nonTerminal : production_rules.keySet()) {
            symbols.add(nonTerminal);
            for (List<String> rule : production_rules.get(nonTerminal)) {
                symbols.addAll(rule);
            }
        }
        symbols.remove(EPSILON);
        symbols.add("$");
        int width = 30;
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // print header
            writer.write(centerText("State", width));
            for (String symbol : symbols) {
                writer.write(centerText(symbol, width));
            }
            // print rows
            for (Integer state : parsingTable.keySet()) {
                writer.newLine();
                writer.write(centerText(state.toString(), width));
                for (String symbol : symbols) {
                    if (parsingTable.get(state).containsKey(symbol)) {
                        writer.write(centerText(parsingTable.get(state).get(symbol), width));
                    } else {
                        writer.write(centerText("", width));
                    }
                }
            }
            System.out.println("Data written to " + filename + " successfully.");
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public void displayItemSets(){
        for (Integer itemSetId : itemSets.keySet()) {
            System.out.print("I"+itemSetId+" => ");
            Map<String, List<List<String>>> itemSet = itemSets.get(itemSetId);
            System.out.print("{ ");
            boolean flag=false;
            for (String nonTerminal : itemSet.keySet()) {
                if(flag==true)
                    System.out.print("\t");
                else
                    flag=true;
                for (List<String> rule : itemSet.get(nonTerminal)) {
                    System.out.print(nonTerminal+" -> ");
                    for (String symbol : rule) {
                        System.out.print(symbol+" ");
                    }
                    System.out.print(" # ");
                }
                System.out.println();
            }
            System.out.println("\t}\n");
        }
    }

   public void displayGotoTable(String filename) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
        for (Integer state : gotoTable.keySet()) {
            for (String symbol : gotoTable.get(state).keySet()) {
                int state_no = gotoTable.get(state).get(symbol);
                writer.write("goto(" + state + "," + symbol + ")   =>   I" + state_no);
                Map<String, List<List<String>>> itemSet = itemSets.get(state_no);
                writer.write("{ ");
                boolean flag = false;
                for (String nonTerminal : itemSet.keySet()) {
                    if (flag == true)
                        writer.write("\t\t\t");
                    else
                        flag = true;
                    for (List<String> rule : itemSet.get(nonTerminal)) {
                        writer.write(nonTerminal + " -> ");
                        for (String symboll : rule) {
                            writer.write(symboll + " ");
                        }
                        writer.write(" # ");
                    }
                    writer.newLine();
                }
                writer.write("\t\t\t}\n\n");
            }
        }
        System.out.println("Data written to " + filename + " successfully.");
    } catch (IOException e) {
        System.err.println("Error writing to file: " + e.getMessage());
    }
}

    public void displayFirstAndFollowPosTable(){
        System.out.println(centerText("Symbol", 10) + centerText("FIRST", 110) + centerText("FOLLOW", 110)+"\n\n");
        for (String nonTerminal : nonTerminalKeys) {
            System.out.println(centerText(nonTerminal, 10)+centerText(findFirst(nonTerminal).toString(), 110)+centerText(findFollow(nonTerminal).toString(), 110)+"\n");
        }
        System.out.println();
    }

    public void displayNumberedProductionRules(){
        int i=1;
        for (String nonTerminal : production_rules_numbered.keySet()) {
            System.out.print(i++ +": "+nonTerminal+" -> ");
            for (String symbol : production_rules_numbered.get(nonTerminal)) {
                System.out.print(symbol+" ");
            }
            System.out.println();
        }
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
    


    // public static void main(String[] args) throws Exception {
    //     // ? Read the grammar from CFG.txt
    //     Path fileName = Path.of("./CFG_test_2.txt");
    //     SLR slr = new SLR();
    //     SLR.START_SYMBOL = "E";
    //     slr.readProductions(fileName);
    //     // System.out.println(slr.production_rules_numbered);
    //     // slr.computeFirstPos();
    //     // System.out.println(slr.findFirst("S"));
    //     // ? Prepare followpos and firstpos
    //     // System.out.println(slr.findFollow("S"));
    //     // System.out.println(slr.findFollow("B"));
    //     // System.out.println(slr.findFollow("C"));
    //     // System.out.println(slr.findFollow("D"));
    //     // System.out.println(slr.findFollow("E"));
    //     // System.out.println(slr.findFollow("F"));

    //     // Prepare LR(0) items
    //     // System.out.println(slr.closure("E"));

    //     // slr.preapareFirstSet();
    //     slr.generateItemSets();
    //     // System.out.println(slr.itemSets);
    //     // System.out.println(slr.gotoTable);
    //     slr.generateParsingTable();
    //     System.out.println(slr.parsingTable);
    //     slr.parseInput("id * id + id");
    //     // slr.displayParsingTable();

    //     // Prepare the parsing table

    // }
}
