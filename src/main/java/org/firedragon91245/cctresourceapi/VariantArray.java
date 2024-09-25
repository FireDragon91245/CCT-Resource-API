package org.firedragon91245.cctresourceapi;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
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

    @SuppressWarnings("unchecked")
    public static <A, B> VariantArray<A, B> ofA(List<?> a, Class<?> aClass) {
        VariantArray<A, B> variantArray = new VariantArray<>();
        variantArray.a = a.toArray((A[]) Array.newInstance(aClass, a.size()));
        return variantArray;
    }

    @SuppressWarnings("unchecked")
    public static <A, B> VariantArray<A, B> ofB(List<?> b, Class<?> bClass) {
        VariantArray<A, B> variantArray = new VariantArray<>();
        variantArray.b = b.toArray((B[]) Array.newInstance(bClass, b.size()));
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
