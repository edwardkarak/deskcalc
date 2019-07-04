package net.magnistudios.deskcalculator;

public class SyntaxError extends RuntimeException
{
    SyntaxError(String msg)
    {
        super(msg);
    }
}