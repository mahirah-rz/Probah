package probah.tac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class TACOptimizer {

    private final Map<String, String> constants = new HashMap<>();

    public List<TACInstruction> optimize(List<TACInstruction> input) {

        constants.clear();

        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        List<TACInstruction> optimized = new ArrayList<>();

        for (TACInstruction instruction : input) {

            String line = instruction.getText().trim();

            if (line.isEmpty()) {
                continue;
            }

           
            if (isLabel(line)) {
                optimized.add(new TACInstruction(line));

               
                constants.clear();

                continue;
            }

            if (line.startsWith("goto ")) {
                optimized.add(
                        new TACInstruction(
                                replaceConstants(line)
                        )
                );

                constants.clear();

                continue;
            }

            
            if (line.startsWith("ifFalse ")) {

                optimized.add(
                        new TACInstruction(
                                replaceConstants(line)
                        )
                );

                continue;
            }

           
            if (line.startsWith("print ")) {

                optimized.add(
                        new TACInstruction(
                                replaceConstants(line)
                        )
                );

                continue;
            }

           
            if (line.contains(" = ")) {

                String optimizedLine = optimizeAssignment(line);

                if (optimizedLine != null && !optimizedLine.isEmpty()) {
                    optimized.add(
                            new TACInstruction(optimizedLine)
                    );
                }

                continue;
            }

           
            optimized.add(new TACInstruction(line));
        }

        return Collections.unmodifiableList(optimized);
    }

    private String optimizeAssignment(String line) {

        int equalsIndex = line.indexOf(" = ");

        if (equalsIndex < 0) {
            return line;
        }

        String left = line.substring(0, equalsIndex).trim();
        String right = line.substring(equalsIndex + 3).trim();

        
        right = replaceConstants(right);

        
        String folded = foldConstantExpression(right);

        if (folded != null) {
            right = folded;
        }

        
        right = simplifyAlgebraicExpression(right);

     
        if (isConstant(right)) {

            constants.put(left, right);

            return left + " = " + right;
        }

        if (isIdentifier(right)) {

            String knownValue = constants.get(right);

            if (knownValue != null) {

                constants.put(left, knownValue);

                return left + " = " + knownValue;
            }
        }

      
        constants.remove(left);

        return left + " = " + right;
    }

   
    
    private String replaceConstants(String text) {

        String result = text;

        for (Map.Entry<String, String> entry : constants.entrySet()) {

            String variable = entry.getKey();
            String value = entry.getValue();

            result = result.replaceAll(
                    "\\b" + Pattern.quote(variable) + "\\b",
                    value
            );
        }

        return result;
    }

    
    
    private String foldConstantExpression(String expression) {

        String expr = expression.trim();

        String[] parts = expr.split("\\s+");

       
        if (parts.length != 3) {
            return null;
        }

        String left = parts[0];
        String operator = parts[1];
        String right = parts[2];

        if (!isInteger(left) || !isInteger(right)) {
            return null;
        }

        int a = Integer.parseInt(left);
        int b = Integer.parseInt(right);

        int result;

        switch (operator) {

            case "+":
                result = a + b;
                return Integer.toString(result);

            case "-":
                result = a - b;
                return Integer.toString(result);

            case "*":
                result = a * b;
                return Integer.toString(result);

            case "/":

               
                if (b == 0) {
                    return null;
                }

                result = a / b;
                return Integer.toString(result);

            case "<":
                return Boolean.toString(a < b);

            case "<=":
                return Boolean.toString(a <= b);

            case ">":
                return Boolean.toString(a > b);

            case ">=":
                return Boolean.toString(a >= b);

            case "==":
                return Boolean.toString(a == b);

            case "!=":
                return Boolean.toString(a != b);

            default:
                return null;
        }
    }

    
    private String simplifyAlgebraicExpression(String expression) {

        String expr = expression.trim();

        String[] parts = expr.split("\\s+");

        if (parts.length != 3) {
            return expression;
        }

        String left = parts[0];
        String operator = parts[1];
        String right = parts[2];

        // x + 0 -> x
        if (operator.equals("+") && right.equals("0")) {
            return left;
        }

        // 0 + x -> x
        if (operator.equals("+") && left.equals("0")) {
            return right;
        }

        // x - 0 -> x
        if (operator.equals("-") && right.equals("0")) {
            return left;
        }

        // x * 1 -> x
        if (operator.equals("*") && right.equals("1")) {
            return left;
        }

        // 1 * x -> x
        if (operator.equals("*") && left.equals("1")) {
            return right;
        }

        // x / 1 -> x
        if (operator.equals("/") && right.equals("1")) {
            return left;
        }

        // x * 0 -> 0
        if (operator.equals("*") && right.equals("0")) {
            return "0";
        }

        // 0 * x -> 0
        if (operator.equals("*") && left.equals("0")) {
            return "0";
        }

        return expression;
    }

    
    private boolean isConstant(String value) {

        return isInteger(value)
                || value.equals("true")
                || value.equals("false");
    }

    private boolean isInteger(String value) {

        return value.matches("-?\\d+");
    }

    
    private boolean isIdentifier(String value) {

        return value.matches("[A-Za-z_][A-Za-z0-9_]*")
                || value.matches("[\\p{L}_][\\p{L}\\p{N}_]*");
    }

  
    private boolean isLabel(String line) {

        return line.matches("L\\d+:");
    }
}