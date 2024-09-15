package org.firedragon91245.cctresourceapi;

import javax.annotation.Nullable;
import java.util.List;

public class VariantArray<A, B> {
    @Nullable
    private A[] a;
    @Nullable
    private B[] b;

    public VariantArray() {
        this.a = null;
        this.b = null;
    }

    public static <A, B> VariantArray<A, B> ofA(A[] a) {
        VariantArray<A, B> variantArray = new VariantArray<>();
        variantArray.a = a;
        return variantArray;
    }

    public static <A, B> VariantArray<A, B> ofB(B[] b) {
        VariantArray<A, B> variantArray = new VariantArray<>();
        variantArray.b = b;
        return variantArray;
    }

    public static <A, B> VariantArray<A, B> ofA(List<A> a)
    {
        VariantArray<A, B> variantArray = new VariantArray<>();
        variantArray.a = (A[]) a.toArray();
        return variantArray;
    }

    public static <A, B> VariantArray<A, B> ofB(List<B> b)
    {
        VariantArray<A, B> variantArray = new VariantArray<>();
        variantArray.b = (B[]) b.toArray();
        return variantArray;
    }

    public boolean isA() {
        return a != null;
    }

    public A[] getA() {
        return a;
    }

    public B[] getB() {
        return b;
    }
}
