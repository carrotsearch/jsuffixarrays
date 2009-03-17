package org.jsuffixarrays;

/**
 * An enum with constants indicating algorithms and their variants. This is of
 * little practical use, but a handy factory method {@link #getInstance()} is
 * also provided.
 */
public enum Algorithm {
    /** Karkkainen-Sanders. */
    KS("Kärkkäinen-Sanders"),

    /** Karkkainen-Sanders, with decorators allowing arbitrary input. */
    KS_D("Kärkkäinen-Sanders (decorated for arbitrary input symbols)"),

    /** Naive sort (quicksort on primitive arrays). */
    NS_2("Naive sort using primitive arrays"),

    /** Yuta Mori's divsufsort algorithm. */
    MORI("Mori's algorithm"),

    /** Yuta Mori's implementation of SA-IS. */
    SAIS("SA-IS algorithm"),

    /** "Larrson-Sadakane qsufsort algorithm */
    QSUFSORT("Larrson-Sadakane qsufsort algorithm");

    /** Full name of the algorithm. */
    private final String name;

    /*
     * 
     */
    private Algorithm(String name) {
        this.name = name;
    }

    /**
     * @return Create and return an algorithm instance.
     */
    public ISuffixArrayBuilder getInstance() {
        switch (this) {
        case KS:
            return new KarkkainenSanders();

        case KS_D:
            return new NonNegativeCompactingDecorator(
                    new ExtraCellsZeroIndexDecorator(new KarkkainenSanders(), 3));

        case NS_2:
            return new NaiveSort2();

        case MORI:
            return new Mori();

        case SAIS:
            return new SAIS();

        case QSUFSORT:
            return new QSufSort();
        }

        throw new RuntimeException("No algorithm for constant: " + this);
    }

    /**
     * Return the full name of the algorithm.
     */
    public String getName() {
        return name;
    }
}
