package net.magnistudios.deskcalculator;

public class WrongNumOperands extends SyntaxError
{
    WrongNumOperands() {
        super("Wrong number of operands");
    }
}