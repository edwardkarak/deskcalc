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

public class Digits extends Fragment
{
    public KeyHandling.Translator translator;

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        translator = KeyHandling.setUpTranslator(context);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.layout_digits, container, false);
        loopRows((LinearLayout) v);
        return v;
    }

    private void loopRows(LinearLayout layout)
    {
        for (int i = 0; i < layout.getChildCount(); ++i) {
            View v = layout.getChildAt(i);
            if (v instanceof LinearLayout) {
                labelButtons((LinearLayout) v);
            }
        }
    }

    private void labelButtons(LinearLayout row)
    {
        for (int i = 0; i < row.getChildCount(); ++i) {
            View v = row.getChildAt(i);
            if (v instanceof Button) {
                toEachDigitKey((Button) v);
            }
        }
    }

    private String digKey2digSym(String idKey)
    {
        return Character.toString(idKey.charAt(idKey.length() - 1));
    }

    private void toEachDigitKey(Button digitBtn)
    {
        String viewId = KeyHandling.getViewId(getActivity(), digitBtn);

        if (viewId.startsWith("dig"))
            digitBtn.setText(digKey2digSym(viewId));

        digitBtn.setOnClickListener(view ->
        {
            MainActivity main = (MainActivity) getActivity();

            String symval = KeyHandling.getSymFromViewId(getActivity(), false, viewId);

            main.vibrate();

            int curPos = main.window.getSelectionEnd();

            if (curPos == main.window.getText().length()) {
                translator.append(symval);
                main.window.setSelection(main.window.getText().length());
            } else {
                translator.insert(curPos, symval);
                main.window.setSelection(curPos + symval.length());
            }
        });
    }
}
