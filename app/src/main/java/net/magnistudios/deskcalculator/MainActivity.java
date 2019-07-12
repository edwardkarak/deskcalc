package net.magnistudios.deskcalculator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.myapp.deskcalculator.R;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;

public class MainActivity extends AppCompatActivity
        implements KeyHandling.Translator
{
    public final int MILLIS_VIBE_DURN = 10;
    public final int SCALE = 24;
    public int nDecFigs = 9;
    public BigDecimal ans = BigDecimal.ZERO;
    public boolean vibeOn = true;
    EditText window;
    TextView angleView;
    AngleMode angleMode;
    TrigMode trigMode = TrigMode.CIRC;
    private boolean outputHasDigSeps = false;

    @NonNull
    public static BigDecimal mkBd(String x)
    {
        return new BigDecimal(x);
    }

    public boolean isInRadiansMode()
    {
        return this.angleMode == AngleMode.RAD;
    }

    String getResStr(int id)
    {
        return getResources().getString(id);
    }

    String angleModeStr(AngleMode mode)
    {
        switch (mode) {
            case DEG:
                return getResStr(R.string.degMode);
            case RAD:
                return getResStr(R.string.radMode);
            case GRAD:
                return getResStr(R.string.gradMode);
        }
        return getResStr(R.string.initAngleViewTxt);    // impossible
    }

    BigDecimal toRad(BigDecimal angle)
    {
        switch (angleMode) {
            case DEG:
                return angle.multiply(piDivBy(mkBd("180")));
            case GRAD:
                return angle.multiply(mkBd("0.015708"));
        }
        return angle;
    }

    BigDecimal toDeg(BigDecimal angle)
    {
        switch (angleMode) {
            case RAD:
                BigDecimal ret = angle.multiply(mkBd("180")).
                        divide(pi(), MathContext.DECIMAL128);
                return ret.setScale(9, BigDecimal.ROUND_HALF_UP);
            case GRAD:
                return angle.multiply(mkBd("0.9"));
        }
        return angle;
    }

    BigDecimal radsToCurrent(BigDecimal radians)
    {
        switch (angleMode) {
            case DEG:
                return radians.multiply(div(mkBd("180"), pi()));
            case GRAD:
                return radians.multiply(div(mkBd("200"), pi()));
        }
        return radians;
    }

    private void createOKDlg(String title, String msg, int iconId)
    {
        AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setIcon(iconId);
        ad.setPositiveButton(getResStr(R.string.posBtn),
                             (dialogInterface, i) -> dialogInterface.dismiss());
        ad.show();
    }

    private void createOKDlg(String title, String msg)
    {
        AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
        ad.setTitle(title);
        ad.setMessage(msg);
        ad.setPositiveButton(getResStr(R.string.posBtn),
                             (dialogInterface, i) -> dialogInterface.dismiss());
        ad.show();
    }

    @Override
    public void append(String newText)
    {
        BasicOps bo = (BasicOps) getSupportFragmentManager().findFragmentById(R.id.basicOps);
        bo.setEnterPressedBefore(false);

        /* if the display text currently contains digit separators, they are
        stripped from the display text */
        if (outputHasDigSeps)
            window.setText(recreateWithoutDigSeparators(window.getText().
                    toString()));
        window.append(newText);
    }

    @Override
    public void insert(int curPos, String newText)
    {
        BasicOps bo = (BasicOps) getSupportFragmentManager().findFragmentById(R.id.basicOps);
        bo.setEnterPressedBefore(false);

        /* if the display text currently contains digit separators, they are
        stripped from the display text */
        if (outputHasDigSeps)
            window.setText(recreateWithoutDigSeparators(window.getText().
                    toString()));

        String windowText = window.getText().toString();

        String finalWindowText = windowText.substring(0, curPos) +
                newText +
                windowText.substring(curPos);

        window.setText(finalWindowText);
    }

    @Override
    public void clear()
    {
        BasicOps bo = (BasicOps) getSupportFragmentManager().findFragmentById(R.id.basicOps);
        bo.setEnterPressedBefore(false);

        window.setText("");
    }

    @Override
    public void del(int curPos)
    {
        BasicOps bo = (BasicOps) getSupportFragmentManager().findFragmentById(R.id.basicOps);
        bo.setEnterPressedBefore(false);

        String text = window.getText().toString().substring(0, curPos);
        int len = text.length();

        if (len == 0)
            return;

        char last = text.charAt(len - 1);
        if (Character.isDigit(last) || last == '.' || last == ')'
                || ShuntingYard.isOperator(String.valueOf(last)) || last == ',' || last == 'π')
            text = text.substring(0, len - 1);

        else if (last == '(') {
            if (len >= 6 && ShuntingYard.isFunc(text.substring(len - 6, len - 1)))
                text = text.substring(0, len - 6);
            else if (len >= 5 && ShuntingYard.isFunc(text.substring(len - 5, len - 1)))
                text = text.substring(0, len - 5);
            else if (len >= 4 && ShuntingYard.isFunc(text.substring(len - 4, len - 1)))
                text = text.substring(0, len - 4);
            else if (len >= 3 && ShuntingYard.isFunc(text.substring(len - 3, len - 1)))
                text = text.substring(0, len - 3);
            else
                text = text.substring(0, len - 1);
        }

        else if (text.substring(len - 3, len).equals("Mod") ||
                text.substring(len - 3, len).equals("Ans")) {
            text = text.substring(0, len - 3);
        }

        window.setText(text + window.getText().toString().substring(curPos));
        if ((len = text.length()) == 0)
            return;
        window.setSelection(len);
    }

    @Override
    public void invOn()
    {
        BasicOps bo = (BasicOps) getSupportFragmentManager().findFragmentById(R.id.basicOps);
        bo.setEnterPressedBefore(false);

        ToggleButton togg = (ToggleButton) findViewById(R.id.toggInv);
        togg.setTextColor(getResources().getColor(R.color.colorAccent));

        Button sciNotOrFuncLog = (Button) findViewById(R.id.opSciNotOrFuncLog);
        sciNotOrFuncLog.setText(getResources().getString(R.string.funcLog));
        sciNotOrFuncLog.setTextColor(getResources().getColor(
                R.color.colorAccent));

        Button funcExpOrFuncLog = (Button) findViewById(R.id.funcExpOrFuncLn);
        funcExpOrFuncLog.setText(getResources().getString(R.string.funcLn));
        funcExpOrFuncLog.setTextColor(getResources().getColor(
                R.color.colorAccent));

        Button opModOrOpFact = (Button) findViewById(R.id.opModOrOpFact);
        opModOrOpFact.setText(getResources().getString(R.string.opFact));
        opModOrOpFact.setTextColor(getResources().getColor(
                R.color.colorAccent));

        ImageButton funcSqrtOrFuncNrt = findViewById(
                R.id.funcSqrtOrFuncNrt);
        funcSqrtOrFuncNrt.setImageResource(R.drawable.ic_nrt);

        Button lcm = findViewById(R.id.funcGcfOrFuncLcm);
        lcm.setText(getResources().getString(R.string.funcLcm));
        lcm.setTextColor(getResources().getColor(R.color.colorAccent));

        Button trigFunc = findViewById(R.id.funcSinOrFuncAsin);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcAsin : R.string.funcAsinh));
        trigFunc.setTextColor(getResources().getColor(R.color.colorAccent));

        trigFunc = findViewById(R.id.funcCosOrFuncAcos);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcAcos : R.string.funcAcosh));
        trigFunc.setTextColor(getResources().getColor(R.color.colorAccent));

        trigFunc = findViewById(R.id.funcTanOrFuncAtan);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcAtan : R.string.funcAtanh));
        trigFunc.setTextColor(getResources().getColor(R.color.colorAccent));
    }

    @Override
    public void invOff()
    {
        BasicOps bo = (BasicOps) getSupportFragmentManager().findFragmentById(R.id.basicOps);
        bo.setEnterPressedBefore(false);

        int clr;
        // log->EE; ln->exp; lcm->gcf; asin->sin; acos->cos; atan->tan; asinh->sinh; acosh->cosh; atanh->tanh
        Button sciNotOrFuncLog = (Button) findViewById(R.id.opSciNotOrFuncLog);
        sciNotOrFuncLog.setText(getResources().getString(R.string.sciNot));
        sciNotOrFuncLog.setTextColor(clr = getResources().getColor(
                R.color.colorCalcBtn));

        ToggleButton togg = (ToggleButton) findViewById(R.id.toggInv);
        togg.setTextColor(clr);

        Button funcExpOrFuncLog = (Button) findViewById(R.id.funcExpOrFuncLn);
        funcExpOrFuncLog.setText(getResources().getString(R.string.funcExp));
        funcExpOrFuncLog.setTextColor(clr);

        Button opModOrOpFact = (Button) findViewById(R.id.opModOrOpFact);
        opModOrOpFact.setText(getResources().getString(R.string.opMod));
        opModOrOpFact.setTextColor(clr);

        ImageButton funcSqrtOrFuncNrt = (ImageButton) findViewById(
                R.id.funcSqrtOrFuncNrt);
        funcSqrtOrFuncNrt.setImageResource(R.drawable.ic_sqrt);

        Button gcf = (Button) findViewById(R.id.funcGcfOrFuncLcm);
        gcf.setText(getResources().getString(R.string.funcGcf));
        gcf.setTextColor(clr);

        Button trigFunc = (Button) findViewById(R.id.funcSinOrFuncAsin);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcSin : R.string.funcSinh));
        trigFunc.setTextColor(clr);

        trigFunc = (Button) findViewById(R.id.funcCosOrFuncAcos);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcCos : R.string.funcCosh));
        trigFunc.setTextColor(clr);

        trigFunc = (Button) findViewById(R.id.funcTanOrFuncAtan);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcTan : R.string.funcTanh));
        trigFunc.setTextColor(clr);
    }

    @Override
    public void enterPressed(String expr)
    {
        new CalculationTask(this).execute(expr);
    }

    public void vibrate()
    {
        if (vibeOn)
            vibrateAlways();
    }

    public void vibrateAlways()
    {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        assert v != null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createOneShot(MILLIS_VIBE_DURN, VibrationEffect.DEFAULT_AMPLITUDE));
        else
            //deprecated in API 26
            v.vibrate(MILLIS_VIBE_DURN);
    }

    private String recreateWithoutDigSeparators(String withDigSeps)
    {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < withDigSeps.length(); ++i)
            if (withDigSeps.charAt(i) != ',')
                res.append(withDigSeps.charAt(i));
        return res.toString();
    }

    // TODO: Implement Help
    // TODO: Implement History

    // TODO: When in Split screen mode, hide scientific

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menuAbout:
                createOKDlg(getResources().getString(R.string.aboutTitle),
                            getResources().getString(R.string.aboutMsg),
                            R.mipmap.ic_launcher);
                return true;

            case R.id.menuSetCirc:
                item.setChecked(false);
                trigMode = TrigMode.CIRC;
                break;
            case R.id.menuSetHyp:
                item.setChecked(false);
                trigMode = TrigMode.HYP;
                break;

            case R.id.menuVibeSettings:
                VibeSettings vs = new VibeSettings();
                vs.show(this.getFragmentManager(), "Y");
                break;

            case R.id.menuSci: {
                item.setChecked(false);

                Scientific sci = (Scientific) getSupportFragmentManager().findFragmentById(R.id.scientific);
                Objects.requireNonNull(sci.getView()).setVisibility(View.VISIBLE);
                break;
            }
            case R.id.menuStd:
                item.setChecked(false);

                Scientific sci = (Scientific) getSupportFragmentManager().findFragmentById(R.id.scientific);
                Objects.requireNonNull(sci.getView()).setVisibility(View.GONE);
                break;
        }

        Scientific sci = (Scientific) getSupportFragmentManager().
                findFragmentById(R.id.scientific);
        sci.labelTrigBtns(trigMode);

        return super.onOptionsItemSelected(item);
    }

    public boolean isHypMode()
    {
        return trigMode == TrigMode.HYP;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        menu.findItem(R.id.menuSci).setChecked(true);
        menu.findItem(R.id.menuSetCirc).setChecked(true);

        return true;
    }

    public String getPreferenceValue()
    {
        SharedPreferences sp = getSharedPreferences("angleMode",0);
        return sp.getString("myStore", AngleMode.DEG.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context ctx = getApplicationContext();
        window = findViewById(R.id.window);
        disableKeybd();

        switch (getPreferenceValue()) {
            case "DEG":
                angleMode = AngleMode.DEG;
                break;
            case "RAD":
                angleMode = AngleMode.RAD;
                break;
            case "GRAD":
                angleMode = AngleMode.GRAD;
                break;
        }

        angleView = findViewById(R.id.angleView);
        angleView.setOnClickListener(new View.OnClickListener()
        {
            public void writeToPreference(String prefData)
            {
                SharedPreferences.Editor editor = getSharedPreferences("angleMode",0).edit();
                editor.putString("myStore", prefData);
                editor.apply();
            }
            @Override
            public void onClick(View view) {
                switch (angleMode) {
                    case DEG:
                        angleMode = AngleMode.RAD;
                        break;
                    case RAD:
                        angleMode = AngleMode.GRAD;
                        break;
                    case GRAD:
                        angleMode = AngleMode.DEG;
                        break;
                }
                angleView.setText(angleModeStr(angleMode));
                writeToPreference(angleMode.toString());
            }
        });
        angleView.setText(angleModeStr(angleMode));

        window.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void afterTextChanged(Editable s)
            {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count)
            {

            }
        });

        ans = ans.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
    }

    private void disableKeybd()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) // >= API 21
            window.setShowSoftInputOnFocus(false);
        else // API 11-20
            window.setTextIsSelectable(true);
    }

    private int numIntDigits(BigDecimal d)
    {
        String asStr = d.toString();
        String arr[] = asStr.split("\\.");
        return arr[0].length();
    }

    public String fmt(BigDecimal d)
    {
        if (d.compareTo(BigDecimal.ZERO) == 0)
            return "0";

        DecimalFormat df;
        // If num. integral digits > 6, use sci. not.
        if (numIntDigits(d) > 6)
            df = new DecimalFormat("#.#E0");
        else
            df = new DecimalFormat("");

        df.setMaximumFractionDigits(nDecFigs);
        df.setRoundingMode(RoundingMode.HALF_UP);
        String ret = df.format(d);

        /* search the formatted output string (containing the result of the
           calculation) for digit separator chars (commas) */
        outputHasDigSeps = ret.contains(",");

        return ret;
    }

    // x must be a non-neg. integer
    public BigDecimal fact(BigDecimal x)
    {
        if (x.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ONE;
        else
            return x.multiply(fact(x.subtract(BigDecimal.ONE)));
    }

    BigDecimal gcd(BigDecimal numerator, BigDecimal denominator)
    {
        return denominator.compareTo(BigDecimal.ZERO) == 0 ?
                numerator :
                gcd(denominator, numerator.remainder(denominator));
    }

    BigDecimal lcd(BigDecimal a, BigDecimal b)
    {
        return div(b, gcd(a, b)).multiply(a);
    }

    public BigDecimal log10(BigDecimal d)
    {
        if (d.compareTo(BigDecimal.ONE) == 0)
            return BigDecimal.ZERO;
        if (d.compareTo(mkBd("1E1")) == 0)
            return mkBd("1");
        if (d.compareTo(mkBd("1E2")) == 0)
            return mkBd("2");
        if (d.compareTo(mkBd("1E3")) == 0)
            return mkBd("3");
        if (d.compareTo(mkBd("1E4")) == 0)
            return mkBd("4");
        if (d.compareTo(mkBd("1E5")) == 0)
            return mkBd("5");
        if (d.compareTo(mkBd("1E6")) == 0)
            return mkBd("6");
        if (d.compareTo(mkBd("1E7")) == 0)
            return mkBd("7");
        if (d.compareTo(mkBd("1E8")) == 0)
            return mkBd("8");

        // log(d) = ln(d)/ln(10)

        final BigDecimal LN10 = mkBd("2.3025850929940456840179914546843642076011014886287729760333279009675726096773524802359972050895982983419677840422862486334095254650828067566662873690987816894829072083255546808437998948262331985283935053089653777326288461633662222876982198867465436674744042432743651550489343149393914796194044002221051017141748003688084012647080685567743216228355220114804663715659121373450747856947683463616792101806445070648000277502684916746550586856935673420670581136429224554405758925724208241314695689016758940256776311356919292033376587141660230105703089634572075440370847469940168269282808481184289314848524948644871927809676271275775397027668605952496716674183485704422507197965004714951050492214776567636938662976979522110718264549734772662425709429322582798502585509785265383207606726317164309505995087807523710333101197857547331541421808427543863591778117054309827482385045648019095610299291824318237525357709750539565187697510374970888692180205189339507238539205144634197265287286965110862571492198849978748873771345686209167058498078280597511938544450099781311469159346662410718466923101075984383191912922307925037472986509290098803919417026544168163357275557031515961135648465461908970428197633658369837163289821744073660091621778505417792763677311450417821376601110107310423978325218948988175979217986663943195239368559164471182467532456309125287783309636042629821530408745609277607266413547875766162629265682987049579549139549180492090694385807900327630179415031178668620924085379498612649334793548717374516758095370882810674524401058924449764796860751202757241818749893959716431055188481952883307466993178146349300003212003277656541304726218839705967944579434683432183953044148448037013057536742621536755798147704580314136377932362915601281853364984669422614652064599420729171193706024449293580370077189810973625332245483669885055282859661928050984471751985036666808749704969822732202448233430971691111368135884186965493237149969419796878030088504089796185987565798948364452120436982164152929878117429733325886079159125109671875109292484750239305726654462762009230687915181358034777012955936462984123664970233551745861955647724618577173693684046765770478743197805738532718109338834963388130699455693993461010907456160333122479493");

        // misleading name--log means natural log

        return div(BigDecimalMath.log(d), LN10);
    }

    public BigDecimal atanh(BigDecimal d)
    {
        // .5 * ln((1+x)/(1-x))
        BigDecimal Half = mkBd("0.5");
        BigDecimal OnePlusX = d.add(BigDecimal.ONE);
        BigDecimal OneMinusX = d.subtract(BigDecimal.ONE);

        return Half.multiply(BigDecimalMath.log(div(OnePlusX, OneMinusX)));
    }

    public BigDecimal pow(BigDecimal x, BigDecimal b)
    {
        if (x.compareTo(BigDecimal.ZERO) < 0) {
            // if exponent is even, (-x)^b == x^b
            if (b.remainder(mkBd("2")).compareTo(BigDecimal.ZERO) == 0)
                return BigDecimalMath.pow(x.negate(), b);
                // if exponent is odd, (-x)^b == -1 * (x^b)
            else
                return BigDecimalMath.pow(x, b).negate();
        }
        else
            return BigDecimalMath.pow(x, b);
    }

    public boolean isWholeNumber(BigDecimal number)
    {
        return number.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal div(BigDecimal a, BigDecimal b)
    {
        return a.divide(b, SCALE, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal piDivBy(BigDecimal den)
    {
        return pi().divide(den, SCALE, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal pi()
    {
        return BigDecimalMath.pi(MathContext.DECIMAL128);
    }

    @Override
    public void rclPressed()
    {
        RecallVar rc = new RecallVar();
        rc.show(this.getFragmentManager(), "a");
    }

    @Override
    public void dms()
    {
        String text = window.getText().toString();
        if (text.length() == 0)
            return;

        BigDecimal x = new CalculationTask(MainActivity.this).doCalc(text);
        BigDecimal dd = toDeg(x);
        if (dd.compareTo(BigDecimal.ZERO) < 0)
            dd = dd.negate();

        BigDecimal whole = dd.setScale(0, RoundingMode.FLOOR);
        BigDecimal mins = ((dd.subtract(whole)).multiply(mkBd("60"))).setScale(0, RoundingMode.FLOOR);
        BigDecimal secs = (dd.subtract(whole).subtract(div(mins, mkBd("60")))).multiply(mkBd("3600"));

        StringBuilder sb = new StringBuilder();

        if (x.compareTo(BigDecimal.ZERO) < 0)
            sb.append("-");
        sb.append(String.valueOf(whole));
        sb.append("° ");

        sb.append(String.valueOf(mins));
        sb.append("' ");

        sb.append(fmt(secs));
        sb.append("\" ");

        window.setText(sb.toString());
    }

    enum TrigMode
    {
        CIRC,
        HYP
    }

    private enum AngleMode
    {
        DEG,
        RAD,
        GRAD,
    }

    public static class VibeSettings extends DialogFragment
    {
        String optionSelected;

        @Override
        public void onActivityCreated(Bundle savedInstanceState)
        {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            MainActivity activity = (MainActivity) getActivity();
            final CharSequence ITEMS[] = {"On", "Off"};

            assert activity != null;

            int defchk = activity.vibeOn ? 0 : 1;
            optionSelected = ITEMS[defchk].toString();

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_vibeSettings);
            builder.setSingleChoiceItems(ITEMS, defchk, (dialogInterface, i) -> {
                optionSelected = ITEMS[i].toString();
                if (optionSelected.equals("On"))
                    activity.vibrateAlways();
            });
            builder.setPositiveButton(R.string.applyBtn, (dialog, id) -> {

                activity.vibeOn = optionSelected.equals(ITEMS[0].toString());
            });
            builder.setNegativeButton(R.string.negBtn, (dialog, id) -> dialog.dismiss());

            return builder.create();
        }
    }

    public static class RecallVar extends DialogFragment
    {
        String optionSelected;
        int idx = -1;

        @Override
        public void onActivityCreated(Bundle savedInstanceState)
        {
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            MainActivity activity = (MainActivity) getActivity();

            assert activity != null;

            String ansFmtted = activity.fmt(activity.ans);

            final CharSequence ITEMS[] = {"Ans = " + ansFmtted};

            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_rclDlg);
            builder.setSingleChoiceItems(ITEMS, -1, (dialogInterface, i) -> {
                if (i == -1)
                    Toast.makeText(activity, R.string.msg_selectvar, Toast.LENGTH_SHORT).show();
                else
                    optionSelected = ITEMS[idx = i].toString();
            });

            if (!ansFmtted.equals("0")) {
                builder.setNeutralButton("Reset", (dialogInterface, i) ->
                        activity.ans = BigDecimal.ZERO);
            }

            builder.setPositiveButton(R.string.btnDoRecall, (DialogInterface dialog, int id) -> {
                if (idx == -1)
                    Toast.makeText(activity, R.string.msg_selectvar, Toast.LENGTH_SHORT).show();
                else {
                    int cursel;
                    if ((cursel = activity.window.getSelectionEnd()) ==
                            activity.window.getText().length())
                        activity.append(ansFmtted);
                    else {
                        activity.insert(cursel, ansFmtted);
                        activity.window.setSelection(cursel + ansFmtted.length());
                    }
                }
            });

            builder.setNegativeButton(R.string.negBtn, (dialog, id) ->
                    dialog.dismiss());

            return builder.create();
        }
    }

    private static class CalculationTask extends AsyncTask<String, Void, Object>
    {
        Throwable e = null;
        private WeakReference<MainActivity> activityReference;

        CalculationTask(MainActivity context)
        {
            activityReference = new WeakReference<>(context);
        }


        private static String remLast(String x)
        {
            return x.substring(0, x.length() - 1);
        }

        // TODO: Next vers.: Implement Imaginary Nos.
        private BigDecimal doCalc(String expr) throws ArithmeticException,
                SyntaxError
        {
            String postfix = ShuntingYard.infix2postfix(expr);
            MainActivity a = activityReference.get();
            String tokens[] = postfix.split("\\s");
            Stack<BigDecimal> stk = new Stack<>();

            for (String tok : tokens) {
                if (tok.equals(a.getResStr(R.string.sym_varAns)))
                    tok = String.format(Locale.getDefault(), "%f", a.ans);//fallthrough to...
                if (ShuntingYard.isNumeric(tok))          // . . . this if statement
                    stk.push(mkBd(tok).setScale(a.SCALE,
                                                BigDecimal.ROUND_HALF_UP));

                if (tok.equals(a.getResStr(R.string.sym_constPi)))
                    stk.push(a.pi());

                if (ShuntingYard.isOperator(tok)) {
                    if (tok.equals(a.getResStr(R.string.sym_opAdd))) {
                        if (stk.size() < 2)
                            throw new WrongNumOperands();
                        stk.push(stk.pop().add(stk.pop()));
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opSub))) {
                        if (stk.size() < 2)
                            throw new WrongNumOperands();
                        BigDecimal op2 = stk.pop();
                        stk.push(stk.pop().subtract(op2));
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opNeg))) {
                        if (stk.size() < 1)
                            throw new WrongNumOperands();
                        BigDecimal op = stk.pop();
                        stk.push(op.negate());
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opMul))) {
                        if (stk.size() < 2)
                            throw new WrongNumOperands();
                        stk.push(stk.pop().multiply(stk.pop()));
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opDiv))) {
                        if (stk.size() < 2)
                            throw new WrongNumOperands();
                        BigDecimal op2 = stk.pop();

                        stk.push(a.div(stk.pop(), op2));
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opPow))) {
                        if (stk.size() < 2)
                            throw new WrongNumOperands();
                        BigDecimal op2 = stk.pop();
                        BigDecimal res = a.pow(stk.pop(), op2);
                        stk.push(res);
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opSquare))) {
                        if (stk.size() < 1)
                            throw new WrongNumOperands();
                        BigDecimal base = stk.pop();
                        stk.push(base.pow(2));
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opMod))) {
                        if (stk.size() < 2)
                            throw new WrongNumOperands();
                        BigDecimal op2 = stk.pop();
                        BigDecimal modulus = stk.pop().remainder(op2);
                        stk.push(modulus);
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opFact))) {
                        if (stk.size() < 1)
                            throw new WrongNumOperands();
                        BigDecimal op = stk.pop();
                        if (op.compareTo(BigDecimal.ZERO) < 0)
                            throw new ArithmeticException("Cannot take " +
                                                                  "negative factorial");
                        if (!a.isWholeNumber(op))
                            throw new ArithmeticException("Cannot take " +
                                                                  "fractional factorial");
                        BigDecimal prod = a.fact(op);
                        stk.push(prod);
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opSciNot))) {
                        switch (stk.size()) {
                            case 0:
                                throw new WrongNumOperands();
                            case 1: // e.g.: E6 equals 1E6
                                stk.push(BigDecimal.ONE.movePointRight(stk.pop().
                                        intValueExact()));
                                break;
                            default:
                                BigDecimal op2 = stk.pop();
                                BigDecimal op1 = stk.pop();
                                stk.push(op1.movePointRight(
                                        op2.intValueExact()));
                        }
                    }

                }
                if (ShuntingYard.isFunc(tok)) {
                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcSqrt)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);
                        BigDecimal radicand = stk.pop();
                        BigDecimal res = BigDecimalMath.sqrt(
                                radicand, MathContext.DECIMAL128);
                        stk.push(res);
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcSin)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal angle = stk.pop();
                        BigDecimal rad = a.toRad(angle);
                        /* if arg is PI/4 rad, return sqrt(2)/2;
                           sin does not work with an angle of PI/4
                           for some reason */
                        if (rad.compareTo(a.piDivBy(mkBd("4"))) == 0)
                            stk.push(a.div(mkBd(String.valueOf(Math.sqrt(2))),
                                    mkBd("2")));
                        // or pi
                        if (rad.compareTo(a.pi()) == 0)
                            stk.push(BigDecimal.ZERO);

                        else {
                            BigDecimal res = BigDecimalMath.sin(rad);
                            stk.push(res);
                        }
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcCos)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal angle = stk.pop();
                        BigDecimal rad = a.toRad(angle);
                        // if arg is PI/4 rad, return sqrt(2)/2; cos does not work with an angle of PI/4 for some reason
                        if (rad.compareTo(a.piDivBy(mkBd("4"))) == 0)
                            stk.push(a.div(mkBd(String.valueOf(Math.sqrt(2))),
                                           mkBd("2")));
                        if (rad.compareTo(a.piDivBy(mkBd("2"))) == 0)
                            stk.push(BigDecimal.ZERO);
                        if (rad.compareTo(a.pi()) == 0)
                            stk.push(mkBd("-1"));
                        else {
                            BigDecimal res = BigDecimalMath.cos(rad);
                            stk.push(res);
                        }
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcTan)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal angle = stk.pop();
                        BigDecimal rad = a.toRad(angle);
                        if (rad.compareTo(a.pi()) == 0)
                            stk.push(BigDecimal.ZERO);
                        else
                            stk.push(BigDecimalMath.tan(rad));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAsin)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.asin(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAcos)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.acos(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAtan)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.atan(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcSinh)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.sinh(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcCosh)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.cosh(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcTanh)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.tanh(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAsinh)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();
                        BigDecimal angle = BigDecimalMath.asinh(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAcosh)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();

                        if (side.compareTo(BigDecimal.ZERO) == 0)
                            throw new ArithmeticException("Imaginary");

                        BigDecimal angle = BigDecimalMath.cosh(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAtanh)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal side = stk.pop();

                        if (side.compareTo(BigDecimal.ONE) == 0)
                            throw new ArithmeticException("Domain");

                        BigDecimal angle = a.atanh(side);
                        stk.push(a.radsToCurrent(angle));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcLog)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        /* logBASEb(x) = ln(x) / ln(b)
                           thus log10(x) = ln(x) / ln(10) */

                        BigDecimal arg = stk.pop();
                        BigDecimal res = a.log10(arg);
                        stk.push(res);
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcLn)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal arg = stk.pop();
                        // misleading name--it is actually ln
                        BigDecimal res = BigDecimalMath.log(arg);
                        stk.push(res);
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcExp)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal arg = stk.pop();
                        stk.push(BigDecimalMath.exp(arg));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcNrt)))) {
                        if (stk.size() < 2)
                            throw new WrongNumArgs(tok, 2);

                        // nrt(n, x) = x ^ (1/n)

                        BigDecimal radicand = stk.pop();
                        BigDecimal n = stk.pop();
                        n = a.div(BigDecimal.ONE, n);
                        stk.push(a.pow(radicand, n));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcGcf)))) {
                        if (stk.size() < 2)
                            throw new WrongNumArgs(tok, 2);

                        BigDecimal op2 = stk.pop();
                        BigDecimal op1 = stk.pop();

                        stk.push(a.gcd(op1, op2));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcLcm)))) {
                        if (stk.size() < 2)
                            throw new WrongNumArgs(tok, 2);

                        BigDecimal op2 = stk.pop();
                        BigDecimal op1 = stk.pop();

                        stk.push(a.lcd(op1, op2));
                    }
                }
            }
            return stk.pop();
        }

        @Override
        protected Object doInBackground(String... strings) throws SyntaxError,
                ArithmeticException
        {
            try {
                return doCalc(strings[0]);
            } catch (Exception ex) {
                e = ex;
            }
            return e;
        }

        protected void onPostExecute(Object result)
        {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // if result is a BigDecimal, the calc. was successful
            // otherwise, we had an error

            if (result instanceof BigDecimal) {
                activity.ans = (BigDecimal) result;

                String fmtted = activity.fmt((BigDecimal) result);
                activity.window.setText(fmtted);

                activity.window.setSelection(activity.window.getText().length());
            }
            else {
                try {
                    if (e != null)
                        throw e;
                } catch (SyntaxError e) {
                    activity.createOKDlg(activity.getResStr(R.string.errTitle_syntax), e.getMessage());
                } catch (ArithmeticException e) {
                    activity.createOKDlg(activity.getResStr(R.string.errTitle_dom), e.getMessage());
                } catch (Throwable e) {     //sometimes get IllegalArgumentException, BigDecimal, precision negative
                    activity.createOKDlg(activity.getResStr(R.string.errTitle_other), e.getMessage());
                }
            }
        }
    }
}