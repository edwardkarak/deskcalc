package net.magnistudios.deskcalculator;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class ShuntingYard
{
    private static final String SUB = "\u2212";
    private static final String NEG = "-";
    private static final String LPAREN = "(";
    private static final String RPAREN = ")";

    // TODO: implement saving of angle mode change
    // Write last angle mode used into file upon exit. Next time app launches,
    // read from this file to set the current angle mode

    // TODO: next vers.: physical constants menu (G, g, Avogadro, mass of earth, etc.)

    /* 1. Parenthesization,
       2. Factorial,
       3. Exponentiation,
       4. Multiplication, unary minus and division,
       5. Addition and subtraction. */
    private static int getOpPrec(String str)
    {
        switch (str) {
            case "(":
            case ")":
                return 4;
            case "E":
            case "!":
                return 3;
            case "^":
            case "²":
                return 2;
            case "/":
            case "*":
            case "Mod":
            case NEG:
                return 1;
            case "+":
            case SUB:
                return 0;
            default:
                return 5;   /* give functions higher precedence, so they may be
                               added to the queue */
        }
    }

    @NonNull
    public static String infix2postfix(String infix) throws SyntaxError
    {
        Stack<String> operators = new Stack<>();
        Queue<String> output = new LinkedList<>();
        ArrayList<String> tokens = tokenize(infix);

        final String ARGSEP = ",";

        for (String tok : tokens) {
            if (tok.equals("Ans") || tok.equals("π") || isNumeric(tok))
                output.offer(tok);
            else if (isUnaryPostfix(tok)) {
                if (operators.size() > 0 && getOpPrec(operators.peek()) > getOpPrec(tok))
                    output.offer(operators.pop());
                output.offer(tok);
            }

            else if (isUnaryPrefix(tok))
                operators.push(tok);
            else if (isFunc(tok))
                operators.push(tok);
            else if (tok.equals(ARGSEP)) {
                while (!operators.empty() && !operators.peek().equals(LPAREN))
                    output.offer(operators.pop());
            }

            else if (isBinary(tok)) {
                if (assocLeft(tok)) {
                    while (!operators.empty() &&
                            getOpPrec(operators.peek()) >= getOpPrec(tok) &&
                            !operators.peek().equals(LPAREN))
                        output.offer(operators.pop());
                }
                else {
                    while (!operators.empty() &&
                            getOpPrec(operators.peek()) > getOpPrec(tok) &&
                            !operators.peek().equals(LPAREN))
                        output.offer(operators.pop());
                }
                operators.push(tok);
            }

            else if (tok.equals(LPAREN))
                operators.push(tok);

            else if (tok.equals(RPAREN)) {
                while (!operators.empty() && !operators.peek().equals(LPAREN))
                    output.offer(operators.pop());

                if (operators.empty() || !operators.peek().equals(LPAREN))
                    throw new ParenthesesMismatch();
                else
                    operators.pop();

                if (!operators.empty() && isFunc(operators.peek()))
                    output.offer(operators.pop());
            }
        }

        while (!operators.empty()) {
            if (operators.peek().equals(LPAREN) ||
                    operators.peek().equals(RPAREN))
                throw new ParenthesesMismatch();
            output.add(operators.pop());
        }

        if (output.size() == 0)
            throw new SyntaxError("Syntax Error");

        return queue2str(output);
    }

    private static String queue2str(Queue<String> x)
    {
        StringBuilder res = new StringBuilder();

        for (String elem : x)
            res.append(elem).append(" ");
        res.deleteCharAt(res.length() - 1);

        return res.toString();
    }

    static boolean shouldMultBeEmitted(TokType tt)
    {
        return tt == TokType.OPERAND || tt == TokType.RPAREN
                || tt == TokType.SQUARED_OPERATOR;
    }

    public static boolean isNumeric(String str)
    {
        int i = 0, len = str.length();
        boolean a = false, b = false, c = false, d = false;
        if (i < len && (str.charAt(i) == '+' || str.charAt(i) == '-')) i++;
        while (i < len && isDigit(str.charAt(i))) {
            i++;
            a = true;
        }
        if (i < len && (str.charAt(i) == '.')) i++;
        while (i < len && isDigit(str.charAt(i))) {
            i++;
            b = true;
        }
        if (i < len && (str.charAt(i) == 'e' || str.charAt(i) == 'E') && (a || b)) {
            i++;
            c = true;
        }
        if (i < len && (str.charAt(i) == '+' || str.charAt(i) == '-') && c) i++;
        while (i < len && isDigit(str.charAt(i))) {
            i++;
            d = true;
        }
        return i == len && (a || b) && (!c || (c && d));
    }

    private static boolean isDigit(char c)
    {
        return c == '0' || c == '1' || c == '2' || c == '3' || c == '4' ||
                c == '5' || c == '6' || c == '7' || c == '8' || c == '9';
    }


    public static boolean isOperator(String str)
    {
        switch (str) {
            case "^":
            case "/":
            case "*":
            case "+":
            case SUB:
            case NEG:
            case "Mod":
            case "!":
            case "E":
            case "²":
                return true;
            default:
                return false;
        }
    }

    public static boolean isBinary(String str)
    {
        switch (str) {
            case "^":
            case "/":
            case "*":
            case "+":
            case SUB:
            case "Mod":
            case "E":
                return true;
            default:
                return false;
        }
    }

    public static boolean isUnaryPostfix(String str)
    {
        return str.equals("!") || str.equals("²");
    }

    public static boolean isUnaryPrefix(String str)
    {
        return str.equals(NEG);
    }

    public static int countInstsOf(String str, String target)
    {
        return (str.length() - str.replace(target, "").length()) / target.length();
    }

    private static void tokenizeCoefAndFunc(String bigTok, ArrayList<String> dest)
    {
        boolean inNum = false;
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < bigTok.length(); ++i) {
            if (Character.isDigit(bigTok.charAt(i)) && !inNum)
                inNum = true;

            if (Character.isLetter(bigTok.charAt(i)) ||
                    bigTok.charAt(i) == '\u221A') {

                if (inNum) {
                    dest.add(buf.toString());
                    dest.add("*");

                    buf.setLength(0);
                    inNum = false;
                }
            }

            buf.append(bigTok.charAt(i));
        }

        if (isFunc(buf.toString()))
            dest.add(buf.toString());
    }

    public static ArrayList<String> tokenize(String in)
    {
        TokType prevTok = TokType.FIRST;
        String regex = "(?<=[-−+^*/()²π!,E])|(?=[-−+^*/()²π!,E])";
        String toks[] = in.split(regex);
        ArrayList<String> ret = new ArrayList<>();
        boolean seenLParen = false;

        for (String tok : toks) {
            if (tok.isEmpty())
                continue;
            if (isNumeric(tok) || tok.equals("Ans") || tok.equals("π")) {
                if (shouldMultBeEmitted(prevTok))
                    ret.add("*");

                prevTok = TokType.OPERAND;
            }
            else if (tok.contains("Ans")) {
                int n = countInstsOf(tok, "Ans");
                ret.add("Ans");
                if (n > 1) {
                    ret.add("^");
                    ret.add(String.valueOf(n));
                }
                continue;
            }
            else if (tok.equals(LPAREN)) {
                seenLParen = true;
                if (shouldMultBeEmitted(prevTok))
                    ret.add("*");

                prevTok = TokType.LPAREN;
            }
            else if (tok.equals(RPAREN)) {
                seenLParen = false;
                prevTok = TokType.RPAREN;
            } else if (isOperator(tok))
                prevTok = tok.equals("²") ?
                        TokType.SQUARED_OPERATOR : TokType.OPERATOR;
            else if (tok.equals(","))
                prevTok = TokType.COMMA;
            else if (isFunc(tok)) {
                if (shouldMultBeEmitted(prevTok))
                    ret.add("*");

                prevTok = TokType.FUNCTION;
            }
            else if (tok.contains("Mod")) {
                String subtoks[] = tok.split("Mod");
                ret.add(subtoks[0]);
                ret.add("Mod");
                ret.add(subtoks[1]);

                continue;
            }
            else {  // coefficient followed by function name
                tokenizeCoefAndFunc(tok, ret);
                continue;
            }

            ret.add(tok);
        }
        if (seenLParen)
            ret.add(")");
        return ret;
    }

    public static boolean isFunc(String str)
    {
        return str.equals("sin") || str.equals("cos") || str.equals("tan")
            || str.equals("asin") || str.equals("acos") || str.equals("atan")
            || str.equals("sinh") || str.equals("cosh") || str.equals("tanh")
            || str.equals("asinh") || str.equals("acosh") || str.equals("atanh")
            || str.equals("\u221A") || str.equals("log") || str.equals("ln")
            || str.equals("exp") || str.equals("nrt") || str.equals("gcf")
            || str.equals("lcm");
    }

    private static boolean assocLeft(String str)
    {
        switch (str) {
            case "^":
            case "²":
            case "E":
            case NEG:
                return false;
            case "/":
            case "*":
            case "+":
            case SUB:
            case "Mod":
            case "!":
                return true;
            case "(":
            case ")":
                return true;
        }
        return true;
    }

    enum TokType
    {
        FIRST,
        OPERAND,
        SQUARED_OPERATOR,
        OPERATOR,
        LPAREN,
        RPAREN,
        COMMA,
        FUNCTION
    }
}
