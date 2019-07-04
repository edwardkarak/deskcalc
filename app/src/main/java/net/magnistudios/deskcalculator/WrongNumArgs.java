package net.magnistudios.deskcalculator;

public class WrongNumArgs extends SyntaxError
{
    WrongNumArgs(String func) {
        super("Function " + func + " requires one argument");
    }
    WrongNumArgs(String func, int required) {
        super("Function " + func + " requires " + required + " arguments");
    }
}