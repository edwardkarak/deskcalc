package net.magnistudios.deskcalculator;

public class MaxArgsExceeded extends SyntaxError {
    MaxArgsExceeded(String funcName, int maxargs)
    {
        super("Function " + funcName + " exceeds the maximum number of arguments, " + maxargs);
    }
}
