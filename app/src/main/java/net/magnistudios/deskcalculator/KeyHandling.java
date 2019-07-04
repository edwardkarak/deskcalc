package net.magnistudios.deskcalculator;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;

public class KeyHandling {
    public interface Translator {
        void append(String text);
        void insert(int curPos, String text);
        void clear();
        void del(int curPos);
        void enterPressed(String expr);
        void invOn();
        void invOff();
        void rclPressed();
        void dms();
    }

    static public Translator setUpTranslator(Context ctx)
    {
        Activity a;
        Translator t;

        if (ctx instanceof Activity){
            a = (Activity) ctx;
            // now that we have the activity, set up the translator object
            try {
                t = (Translator) a;
                return t;
            } catch (ClassCastException e){
                throw new ClassCastException(a.toString());
            }
        }
        return null;
    }

    static public String firstLetLowerCase(String firstLetUpper)
    {
        StringBuilder sb = new StringBuilder(firstLetUpper);

        sb.setCharAt(0, Character.toLowerCase(firstLetUpper.charAt(0)));

        return sb.toString();
    }

    // e.g.: opSciNotOrFuncLog
    @Nullable
    static private String getDiFunction(boolean invPressed, String viewId, String prefix)
    {
        if (!viewId.contains("Or"))
            return viewId;

        String[] difunc = new String[2];

        String stem = viewId.substring(prefix.length());
        for (int i = 1; i < stem.length(); ++i) {
            char c = stem.charAt(i);
            char prev = stem.charAt(i - 1);
            if (c == 'r' && prev == 'O') {
                difunc[0] = stem.substring(0, i - 1);
                difunc[1] = firstLetLowerCase(stem.substring(i + 1));

                if (!difunc[0].startsWith(prefix))
                    difunc[0] = prefix + difunc[0];

                return difunc[invPressed ? 1 : 0];
            }
        }
        return null;
    }

    static public boolean isDeadViewId(boolean isTogglePressed, String viewId)
    {
        String labelId = getLabelId(isTogglePressed, viewId);
        return isDeadKey(labelId);
    }

    // dead keys are those that do not generate anything on the display
    static public boolean isDeadKey(String labelId)
    {
        //if (strRes.startsWith("sym_"))
            // strRes = strRes.substring("sym_".length());

        return labelId.equals("toggInv") || labelId.equals("enter") ||
                labelId.equals("convInPi") || labelId.equals("rcl") ||
                labelId.equals("clr") || labelId.equals("ce") ||
                labelId.equals("bkspc") || labelId.startsWith("imm");
    }


    // VIEW ID:             I.D. of the view
    // LABEL:               The text on the button
    // LABEL ID:            The I.D. for the view label
    // SYMBOL:              The string that is displayed for each button
    // SYMBOL ID:           The I.D. for the symbol text

    static public String getViewId(Context ctx, View v)
    {
        if (v.getId() == View.NO_ID)
            return "";
        return ctx.getResources().getResourceEntryName(v.getId());
    }

    static public String getLabelId(boolean invPressed, String viewId)
    {
        if (viewId.startsWith("dig"))
            return Character.toString(viewId.charAt(viewId.length() - 1));

        if (viewId.startsWith("op"))
            return getDiFunction(invPressed, viewId, "op");

        if (viewId.startsWith("func"))
            return getDiFunction(invPressed, viewId, "func");

        if (viewId.startsWith("togg") || viewId.startsWith("var") ||
                viewId.startsWith("const") || viewId.startsWith("imm") ||
                viewId.equals("enter") || viewId.equals("decpt") ||
                viewId.equals("bkspc") || viewId.equals("clr") ||
                viewId.equals("lparen") || viewId.equals("rparen") ||
                viewId.equals("rcl"))
            return viewId;

        // shouldn't happen
        return viewId;
    }


    static public String getSymId(String labelId)
    {
        if (labelId.length() == 1 && Character.isDigit(labelId.charAt(0)) || isDeadKey(labelId))
            return labelId;
        return "sym_" + labelId;
    }

    static private int getStringIdentifier(Context ctx, String pString)
    {
        return ctx.getResources().getIdentifier(pString, "string",
                                                ctx.getPackageName());
    }
    static public String getStrResText(Context ctx, String idStrRes)
    {
        return ctx.getResources().getString(getStringIdentifier(ctx, idStrRes));
    }

    private static String getSymFromTrigViewIdHyp(boolean isToggleOn, String baseTrigFuncName)
    {
        StringBuilder sb = new StringBuilder();
        if (isToggleOn)
            sb.append("a");
        sb.append(baseTrigFuncName);
        sb.append("h(");

        return sb.toString();
    }

    static public String getSymFromViewId(Context ctx, boolean isToggleOn, String viewId)
    {
        String labelId = KeyHandling.getLabelId(isToggleOn, viewId);
        String symId = KeyHandling.getSymId(labelId);
        if (isDeadKey(labelId) || Character.isDigit(symId.charAt(0)))
            return labelId;
        if (Scientific.isTrigFunc(viewId) && ((MainActivity) ctx).isHypMode()) {
            switch (viewId) {
                case "funcSinOrFuncAsin":
                    return getSymFromTrigViewIdHyp(isToggleOn, "sin");
                case "funcCosOrFuncAcos":
                    return getSymFromTrigViewIdHyp(isToggleOn, "cos");
                case "funcTanOrFuncAtan":
                    return getSymFromTrigViewIdHyp(isToggleOn, "tan");
            }
        }
        return KeyHandling.getStrResText(ctx, symId);
    }
}
