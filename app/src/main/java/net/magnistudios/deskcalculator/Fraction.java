package net.magnistudios.deskcalculator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class Fraction {
    private BigDecimal num, den;
    private BigInteger gcd(BigInteger a, BigInteger b)
    {
        if (b.compareTo(BigInteger.ZERO) == 0)
            return a;
        return gcd(b, a.remainder(b));
    }
    private boolean isIntegerValue(BigDecimal bd) {
        return bd.stripTrailingZeros().scale() <= 0;
    }

    public Fraction(BigDecimal a, BigDecimal b)
    {
        this.num = a;
        this.den = b;
    }

    public void simplify()
    {
        // keep multiplying num and den by 10 until they are both integers
        while (!isIntegerValue(num) || !isIntegerValue(den)) {
            num = num.multiply(BigDecimal.TEN);
            den = den.multiply(BigDecimal.TEN);
        }

        // then simplify the resulting fraction of integers
        BigInteger gcf = gcd(num.toBigInteger(), den.toBigInteger());

        num = num.divide(new BigDecimal(gcf), MathContext.DECIMAL32);
        den = den.divide(new BigDecimal(gcf), MathContext.DECIMAL32);
    }

    public BigDecimal getNum()
    {
        return num;
    }
    public BigDecimal getDen()
    {
        return den;
    }

    public String toString()
    {
        return num + "/" + den;
    }
}
