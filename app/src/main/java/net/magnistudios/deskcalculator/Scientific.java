package net.magnistudios.deskcalculator;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.myapp.deskcalculator.R;

public class Scientific extends Fragment
{
    public KeyHandling.Translator translator;
    private boolean trigFuncWasLastKeyPressed = false;

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);

        translator = KeyHandling.setUpTranslator(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup
            container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.layout_scientific, container, false);
        loopRows((LinearLayout) v);
        return v;
    }
    private void loopRows(LinearLayout layout)
    {
        for (int i = 0; i < layout.getChildCount(); ++i) {
            View v = layout.getChildAt(i);
            if (v instanceof LinearLayout) {
                setHandlers((LinearLayout) v);
            }
        }
    }

    private void trigChangeLabel(MainActivity.TrigMode trigMode, int idBtn)
    {
        Button trigBtn = getView().findViewById(idBtn);

        String lbl = trigBtn.getText().toString();

        if (trigMode == MainActivity.TrigMode.HYP && !lbl.contains("h")) {
            lbl += "h";
            trigBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        }

        else if (trigMode == MainActivity.TrigMode.CIRC && lbl.charAt(lbl.length() - 1) == 'h') {
            lbl = lbl.substring(0, lbl.length() - 1);
            trigBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }

        trigBtn.setText(lbl);
    }

    public void labelTrigBtns(MainActivity.TrigMode trigMode)
    {
        trigChangeLabel(trigMode, R.id.funcSinOrFuncAsin);
        trigChangeLabel(trigMode, R.id.funcCosOrFuncAcos);
        trigChangeLabel(trigMode, R.id.funcTanOrFuncAtan);
    }

    private void setHandlers(LinearLayout root)
    {
        for (int i = 0; i < root.getChildCount(); ++i) {
            View v = root.getChildAt(i);
            if (v instanceof Button || v instanceof ImageButton) {
                v.setOnClickListener(view -> {

                    ((MainActivity) getActivity()).vibrate();

                    String viewId = KeyHandling.getViewId(getActivity(), view);

                    if (isTrigFunc(viewId))
                        trigFuncWasLastKeyPressed = true;

                    if (viewId.equals("constPi") &&
                            !((MainActivity) getActivity()).isInRadiansMode() &&
                            trigFuncWasLastKeyPressed) {
                        Toast.makeText(getActivity(), "Consider switching to " +
                                "Radians mode.", Toast.LENGTH_LONG).show();
                        trigFuncWasLastKeyPressed = false;
                    }

                    String symval = KeyHandling.getSymFromViewId(getActivity(), isToggleOn(), viewId);

                    switch (viewId) {
                        case "toggInv":
                            if (isToggleOn())
                                translator.invOn();
                            else
                                translator.invOff();
                            break;
                        case "rcl":
                            translator.rclPressed();
                            break;
                        case "immDms":
                            translator.dms();
                            break;
                        default:
                            if (KeyHandling.isDeadViewId(isToggleOn(), viewId))
                                throw new RuntimeException("Dead key--should be handled");

                            MainActivity main = (MainActivity) getActivity();
                            int curPos = main.window.getSelectionEnd();

                            if (curPos == main.window.getText().length()) {
                                translator.append(symval);
                                main.window.setSelection(main.window.getText().length());
                            } else {
                                translator.insert(curPos, symval);
                                main.window.setSelection(curPos + symval.length());
                            }
                            break;
                    }
                });
            }
        }
    }
    public boolean isToggleOn()
    {
        ToggleButton togg = getActivity().findViewById(R.id.toggInv);
        return togg.isChecked();
    }
    public static boolean isTrigFunc(String viewId)
    {
        String viewIdLower = viewId.toLowerCase();
        return viewIdLower.contains("sin") || viewIdLower.contains("cos") ||
                viewIdLower.contains("tan");
    }
}
