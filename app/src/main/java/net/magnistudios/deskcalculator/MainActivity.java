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
import android.util.TypedValue;
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
import java.math.BigInteger;
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
            else if (len >= 2 && ShuntingYard.isFunc(text.substring(len - 2, len - 1)))
                text = text.substring(0, len - 2);
            else if (len == 1 && ShuntingYard.isFunc(text.substring(len - 1, len - 1)))
                text = text.substring(0, len - 1);
            else
                text = text.substring(0, len - 1);
        }

        else if (text.substring(len - 1, len).equals("\u221A"))
            text = text.substring(0, len - 1);

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

        int clr = getResources().getColor(
                R.color.colorAccent);

        ToggleButton togg = (ToggleButton) findViewById(R.id.toggInv);
        togg.setTextColor(clr);

        Button sciNotOrFuncLog = (Button) findViewById(R.id.opSciNotOrFuncLog);
        sciNotOrFuncLog.setText(getResources().getString(R.string.funcLog));
        sciNotOrFuncLog.setTextColor(clr);

        Button funcExpOrFuncLog = (Button) findViewById(R.id.funcExpOrFuncLn);
        funcExpOrFuncLog.setText(getResources().getString(R.string.funcLn));
        funcExpOrFuncLog.setTextColor(clr);

        Button opModOrOpFact = (Button) findViewById(R.id.opModOrOpFact);
        opModOrOpFact.setText(getResources().getString(R.string.opFact));
        opModOrOpFact.setTextColor(clr);

        ImageButton funcSqrtOrFuncNrt = findViewById(
                R.id.funcSqrtOrFuncNrt);
        funcSqrtOrFuncNrt.setImageResource(R.drawable.ic_nrt);

        Button lcm = findViewById(R.id.funcGcfOrFuncLcm);
        lcm.setText(getResources().getString(R.string.funcLcm));
        lcm.setTextColor(clr);

        Button trigFunc = findViewById(R.id.funcSinOrFuncAsin);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcAsin : R.string.funcAsinh));
        trigFunc.setTextColor(clr);

        trigFunc = findViewById(R.id.funcCosOrFuncAcos);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcAcos : R.string.funcAcosh));
        trigFunc.setTextColor(clr);

        trigFunc = findViewById(R.id.funcTanOrFuncAtan);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcAtan : R.string.funcAtanh));
        trigFunc.setTextColor(clr);

        trigFunc = findViewById(R.id.immDmsOrFuncAtan2);
        trigFunc.setText(getResources().getString(R.string.funcAtan2));
        trigFunc.setTextColor(clr);
        trigFunc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
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

        trigFunc = findViewById(R.id.funcTanOrFuncAtan);
        trigFunc.setText(getResources().getString(trigMode == TrigMode.CIRC ? R.string.funcTan : R.string.funcTanh));
        trigFunc.setTextColor(clr);

        trigFunc = findViewById(R.id.immDmsOrFuncAtan2);
        trigFunc.setText(getResources().getString(R.string.immDms));
        trigFunc.setTextColor(clr);
        trigFunc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
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

    // TODO: Too many args. are accepted

    // TODO: Save vibration state settings

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

        return div(ln(d), LN10);    // log change of base rule
    }

    public BigDecimal atanh(BigDecimal d)
    {
        // .5 * ln((1+x)/(1-x))
        BigDecimal Half = mkBd("0.5");
        BigDecimal OnePlusX = d.add(BigDecimal.ONE);
        BigDecimal OneMinusX = d.subtract(BigDecimal.ONE);

        return Half.multiply(ln(div(OnePlusX, OneMinusX)));
    }

    public BigDecimal ln(BigDecimal arg)
    {
        try {
            return BigDecimalMath.log(arg);
        } catch (ArithmeticException e) {
            throw new ArithmeticException(getResStr(R.string.err_nonreal));
        }
    }

    public BigDecimal pow(BigDecimal a, BigDecimal b)
    {
        BigDecimal result = BigDecimal.ZERO;

        if (a.compareTo(BigDecimal.ZERO) == 0 && b.compareTo(BigDecimal.ZERO) == 0)
            throw new ArithmeticException(getString(R.string.err_zero_to_zeroth));

        if (b.compareTo(BigDecimal.ONE) == 0)
            return a;

        // in general, a^b == exp(b*ln a). However, we must handle non-positive values of a.
        else {
            if (a.compareTo(BigDecimal.ZERO) == 0) {
                if (b.compareTo(BigDecimal.ZERO) > 0)
                    return BigDecimal.ZERO;
                if (b.compareTo(BigDecimal.ZERO) < 0)
                    throw new ArithmeticException(getResStr(R.string.err_zeroDivisor));
            }

            if (a.compareTo(BigDecimal.ZERO) < 0) {
                // if b is positive fraction
                if (b.compareTo(BigDecimal.ZERO) > 0 && b.compareTo(BigDecimal.ONE) < 0)  {
                    // NOTE: only works for rational exponent
                    // a ^ (p/q) = nrt(q, pow(a, p))
                    Fraction f = new Fraction(b, BigDecimal.ONE);
                    f.simplify();

                    BigInteger n = f.getDen().toBigInteger();
                    BigDecimal radicand = pow(a, f.getNum());
                    if (n.compareTo(new BigInteger("2")) == 0)
                        result = BigDecimalMath.sqrt(radicand);
                    else
                        result = nrt(n, radicand);
                }
                // is exponent is even, (-a)^b == a^b
                if (b.remainder(mkBd("2")).compareTo(BigDecimal.ZERO) == 0)
                    result = BigDecimalMath.exp(b.multiply(BigDecimalMath.log(a.negate())));

                // if exponent is odd, (-a)^b == -(a^b)
                if (b.remainder(mkBd("2")).compareTo(BigDecimal.ZERO) != 0)
                    result = BigDecimalMath.exp(b.multiply(BigDecimalMath.log(a.negate()))).negate();

            }

            if (a.compareTo(BigDecimal.ZERO) > 0)
                result = BigDecimalMath.exp(b.multiply(BigDecimalMath.log(a)));
        }
        return result;
    }

    public BigDecimal nrt(BigInteger n, BigDecimal radicand)
    {
        BigDecimal result;
        if (radicand.compareTo(BigDecimal.ZERO) < 0) {
            if (n.remainder(new BigInteger("2")).compareTo(BigInteger.ZERO) == 0) {
                if (radicand.compareTo(BigDecimal.ZERO) < 0)    // even root of negative number is nonreal
                    throw new ArithmeticException(getResStr(R.string.err_nonreal));
                else
                    result = BigDecimalMath.root(n.intValue(), radicand.negate());
            }
            else
                result = BigDecimalMath.root(n.intValue(), radicand.negate()).negate();
        } else
            result = BigDecimalMath.root(n.intValue(), radicand);

        return result;
    }

    final BigDecimal PI_OV_2 = mkBd("1.570796326794896619231321691639751442098584699687552910487472296153908203143104499314017412671058533991074043256641153323546922304775291115862679704064240558725142051350969260552779822311474477465190982214405487832966723064237824116893391582635600954572824283461730174305227163324106696803630124570636862293503303157794");
    final BigDecimal PI_OV_3 = mkBd("1.047197551196597746154214461093168");
    final BigDecimal PI_OV_4 = mkBd("0.785398163397448309615660845819875721049292349843776455243736148076954101571552249657008706335529266995537021628320576661773461152387645557931339852032120279362571025675484630276389911155737238732595491107202743916483361532118912058446695791317800477286412141730865087152613581662053348401815062285318431146751651578897");
    final BigDecimal PI_OV_6 = mkBd("0.5235987755982988730771072305465838");

    final BigDecimal RT2_OV_2 = mkBd("0.70710678118654752440084436210484903928483593768847403658833986899536623923105351942519376716382078636750692311545614851246241802792536860632206074854996791570661133296375279637789997525057639103028573505477998580298513726729843100736425870932044459930477616461524215435716072541988130181399762570399484362669827316590441482031030762917619752737287514387998086491778761016876592850567718730170424942358019344998534950240751527201389515822712391153424646845931079028923155579833435650650780928449361861764425463243062474885771091671021428430300734123603857175");
    final BigDecimal RT3_OV_2 = mkBd("0.866025403784438646763723170752936183471402626905190314027903489725966508454400018540573093378624287837813070707703351514984972547499476239405827756047186824264046615951152791033987410050542337461632507656171633451661443325336127334460915");
    final BigDecimal HALF = mkBd("0.5");

    // x in radians
    public BigDecimal sin(BigDecimal x)
    {
        if (isWholeNumber(div(x, pi())))
            return BigDecimal.ZERO;     // sin(pi*n) = 0. n an integer

        if  (x.compareTo(PI_OV_2) == 0)
            return BigDecimal.ONE;

        if (x.compareTo(PI_OV_3) == 0)
            return RT3_OV_2;

        if (x.compareTo(PI_OV_4) == 0)
            return RT2_OV_2;

        if (x.compareTo(PI_OV_6) == 0)
            return HALF;

        return BigDecimalMath.sin(x);
    }

    // x in radians
    public BigDecimal cos(BigDecimal x)
    {
        BigDecimal decN = div(x, pi());
        BigInteger n;
        if (isWholeNumber(decN)) {
            // cos(pi*n) = -1 for odd integer n; = +1 for even integer n
            n = decN.toBigInteger();
            if (n.remainder(new BigInteger("2")).compareTo(BigInteger.ZERO) == 0)
                return BigDecimal.ONE;
            else
                return BigDecimal.ONE.negate();
        }

        if  (x.compareTo(PI_OV_2) == 0)
            return BigDecimal.ZERO;

        if (x.compareTo(PI_OV_3) == 0)
            return HALF;

        if (x.compareTo(PI_OV_4) == 0)
            return RT2_OV_2;

        if (x.compareTo(PI_OV_6) == 0)
            return RT3_OV_2;

        return BigDecimalMath.cos(x);
    }

    // x in radians
    public BigDecimal tan(BigDecimal x)
    {
        final BigDecimal RT3 = new BigDecimal("1.73205080756887729352744634150587236694280525381038062805580697945193301690880003708114618675724857567562614141540670302996994509499895247881165551209437364852809323190230558206797482010108467492326501531234326690332288665067225466892183");
        final BigDecimal RECIP_RT3 = new BigDecimal("0.5773502691896257645091487805019575");
        if (isWholeNumber(div(x, pi())))
            return BigDecimal.ZERO;     // tan(pi*n) = 0. n an integer

        if  (x.compareTo(PI_OV_2) == 0)
            throw new ArithmeticException("Tangent of π/2 is undefined");

        if (x.compareTo(PI_OV_3) == 0)
            return RT3;

        if (x.compareTo(PI_OV_4) == 0)
            return BigDecimal.ONE;

        if (x.compareTo(PI_OV_6) == 0)
            return RECIP_RT3;

        return BigDecimalMath.tan(x);
    }

    public boolean isWholeNumber(BigDecimal number)
    {
        return number.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal div(BigDecimal a, BigDecimal b)
    {
        if (b.compareTo(BigDecimal.ZERO) == 0)
            throw new ArithmeticException(getResStr(R.string.err_zeroDivisor));
        else
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
                        stk.push(a.pow(stk.pop(), op2));
                    }

                    if (tok.equals(a.getResStr(R.string.sym_opSquare))) {
                        if (stk.size() < 1)
                            throw new WrongNumOperands();
                        BigDecimal base = stk.pop();
                        stk.push(base.multiply(base));
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
                            throw new ArithmeticException(a.getString(R.string.err_negFact));
                        if (!a.isWholeNumber(op))
                            throw new ArithmeticException(a.getString(R.string.err_negFact));

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

                        stk.push(a.sin(rad));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcCos)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal angle = stk.pop();
                        BigDecimal rad = a.toRad(angle);
                        stk.push(a.cos(rad));
                    }

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcTan)))) {
                        if (stk.size() < 1)
                            throw new WrongNumArgs(tok);

                        BigDecimal angle = stk.pop();
                        BigDecimal rad = a.toRad(angle);
                        stk.push(a.tan(rad));
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

                    if (tok.equals(remLast(a.getResStr(R.string.sym_funcAtan2)))) {
                        if (stk.size() < 2)
                            throw new WrongNumArgs(tok);

                        BigDecimal op2 = stk.pop();
                        BigDecimal op1 = stk.pop();

                        BigDecimal angle = a.radsToCurrent(BigDecimalMath.atan2(op1, op2));
                        stk.push(angle);
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
                        stk.push(a.ln(arg));
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

                        BigDecimal radicand = stk.pop();
                        BigDecimal n = stk.pop();
                        stk.push(a.nrt(n.toBigInteger(), radicand));
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
                } catch (Throwable e) {     // TODO: sometimes get IllegalArgumentException, BigDecimal, precision negative
                    activity.createOKDlg(activity.getResStr(R.string.errTitle_other), e.getMessage());
                }
            }
        }
    }
}
// TODO: import c++ project for unit conversion