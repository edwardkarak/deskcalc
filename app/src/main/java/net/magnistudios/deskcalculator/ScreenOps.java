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

public class ScreenOps extends Fragment
{
    KeyHandling.Translator translator;
    public void onAttach(Context context)
    {
        super.onAttach(context);

        translator = KeyHandling.setUpTranslator(context);
    }

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.layout_screen_ops, container, false);

        setClickListeners((LinearLayout) v);

        return v;
    }
    private void setClickListeners(LinearLayout rl)
    {
        for (int i = 0; i < rl.getChildCount(); ++i) {
            View v = rl.getChildAt(i);
            if (v instanceof Button)
                v.setOnClickListener(this::handleClick);
        }
    }
    private void handleClick(View view)
    {
        MainActivity main = (MainActivity) getActivity();

        main.vibrate();

        String viewId = KeyHandling.getViewId(getActivity(), view);
        switch (viewId) {
            case "bkspc":
                translator.del(main.window.getSelectionEnd());
                break;
            case "clr":
                translator.clear();
                break;
        }
    }
}
