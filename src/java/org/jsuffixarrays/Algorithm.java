package org.jsuffixarrays;

/**
 * An enum with constants indicating algorithms and their variants. This is of
 * little practical use, but a handy factory method {@link #getInstance()} is
 * also provided.
 */
public enum Algorithm {
    /** Karkkainen-Sanders. */
    SKEW("Kärkkäinen-Sanders"),

    /** Karkkainen-Sanders, with decorators allowing arbitrary input. */
    SKEW_D("Kärkkäinen-Sanders (decorated for arbitrary input symbols)"),

    /** Naive sort (quicksort on primitive arrays). */
    NS_2("Naive sort using primitive arrays"),

    /** Yuta Mori's divsufsort algorithm. */
    DIVSUFSORT("Mori's algorithm"),

    /** Yuta Mori's implementation of SA-IS. */
    SAIS("SA-IS algorithm"),

    /** Klaus-Bernd Schürmann's bucket pointer refinement algorithm */
    BPR("Klaus-Bernd Schürmann's bpr algorithm"),

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
        case SKEW:
            return new Skew();

        case SKEW_D:
            return new NonNegativeCompactingDecorator(
                    new ExtraCellsZeroIndexDecorator(new Skew(), 3));

        case NS_2:
            return new NaiveSort2();

        case DIVSUFSORT:
            return new DivSufSort();

        case SAIS:
            return new SAIS();

        case QSUFSORT:
            return new QSufSort();

        case BPR:
            return new BPR();

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
