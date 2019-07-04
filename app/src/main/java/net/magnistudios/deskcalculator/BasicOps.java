package net.magnistudios.deskcalculator;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.myapp.deskcalculator.R;

import java.util.ArrayList;

public class BasicOps extends Fragment
{
    KeyHandling.Translator translator;
    private ArrayList<String> prev;
    private boolean enterPressedBefore = false;

    public void onAttach(Context context)
    {
        super.onAttach(context);

        translator = KeyHandling.setUpTranslator(context);
    }

    public void setEnterPressedBefore(boolean enterPressedBefore1)
    {
        enterPressedBefore = enterPressedBefore1;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.layout_basic_ops, container, false);
        setHandlers((LinearLayout) v);
        return v;
    }
    private void setHandlers(LinearLayout layout)
    {
        for (int i = 0; i < layout.getChildCount(); ++i) {
            View v = layout.getChildAt(i);
            if (v instanceof Button) {
                v.setOnClickListener(view -> {
                    MainActivity main = (MainActivity) getActivity();

                    main.vibrate();

                    String viewId = KeyHandling.getViewId(getActivity(), view);
                    if (viewId.equals("enter")) {

                        String expr = main.window.getText().toString();
                        if (expr.length() > 0) {
                            if (!enterPressedBefore) {
                                translator.enterPressed(expr);

                                prev = ShuntingYard.tokenize(expr);
                                enterPressedBefore = true;
                            } else {
                                if (prev.size() == 1)
                                    return;

                                StringBuilder newExpr = new StringBuilder();
                                newExpr.append("Ans");

                                String secondToLast, last;
                                if (prev.size() > 2 && prev.get(prev.size() - 2).equals("-"))
                                    newExpr.append(prev.get(prev.size() - 3));
                                newExpr.append(secondToLast = prev.get(prev.size() - 2));
                                newExpr.append(last = prev.get(prev.size() - 1));

                                if (!(ShuntingYard.isNumeric(last) || last.equals("π") || last.equals("Ans")) ||!ShuntingYard.isOperator(secondToLast))
                                    return;

                                translator.enterPressed(newExpr.toString());
                            }
                        }
                    } else {
                        String labelId = KeyHandling.getLabelId(false, viewId);
                        String symId = KeyHandling.getSymId(labelId);
                        String symval = KeyHandling.getStrResText(getActivity(), symId);

                        int curPos = main.window.getSelectionEnd();

                        if (curPos == main.window.getText().length()) {
                            translator.append(symval);
                            main.window.setSelection(main.window.getText().length());
                        } else {
                            translator.insert(curPos, symval);
                            main.window.setSelection(curPos + symval.length());
                        }
                    }
                });
            }
        }
    }
}
