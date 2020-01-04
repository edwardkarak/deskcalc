package net.magnistudios.deskcalculator;

class SyntaxError extends RuntimeException
{
    SyntaxError(String msg)
    {
        super(msg);
    }
}