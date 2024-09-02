package org.firedragon91245.cctresourceapi;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OneOrMore<T> {
    private T one;
    private T[] more;

    private OneOrMore()
    {

    }

    @SuppressWarnings("unchecked")
    public static <T> OneOrMore<T> fromMore(Collection<?> more) {
        OneOrMore<T> oneOrMore = new OneOrMore<>();
        oneOrMore.more = (T[]) more.toArray();
        return oneOrMore;
    }

    public static <T> OneOrMore<T> fromOne(T result) {
        OneOrMore<T> oneOrMore = new OneOrMore<>();
        oneOrMore.one = result;
        return oneOrMore;
    }

    public void ifOne(Consumer<T> oneConsumer) {
        if (one != null) {
            oneConsumer.accept(one);
        }
    }

    public void ifMore(Consumer<T[]> moreConsumer) {
        if (more != null) {
            moreConsumer.accept(more);
        }
    }

    // Method to handle both cases with fallback
    public void ifOneOrElse(Consumer<T> oneConsumer, Consumer<List<T>> moreConsumer) {
        if (one != null) {
            oneConsumer.accept(one);
        } else if (more != null) {
            moreConsumer.accept(Arrays.stream(more).collect(Collectors.toList()));
        }
    }
}
