package io.github.vikaschaudharyarit_max.smartdbf.config;

/**
 * Configuration options for opening a DBF file.
 *
 * <p>Use the default constructor for sensible defaults, or the {@link Builder} for
 * explicit configuration:
 *
 * <pre>{@code
 * DbfConfig config = new DbfConfig.Builder()
 *         .bufferSize(65536)
 *         .build();
 *
 * DbfReader reader = Dbf.open("large.dbf", StandardCharsets.ISO_8859_1, config);
 * }</pre>
 */
public class DbfConfig {

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    private final int bufferSize;

    /** Creates a configuration with default settings (64 KB read buffer). */
    public DbfConfig() {
        this.bufferSize = DEFAULT_BUFFER_SIZE;
    }

    private DbfConfig(Builder builder) {
        this.bufferSize = builder.bufferSize;
    }

    /**
     * The size of the internal read buffer in bytes.
     * Larger values can improve throughput when reading large DBF files from disk or network.
     * Default is 65536 (64 KB).
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /** Builder for {@link DbfConfig}. */
    public static class Builder {

        private int bufferSize = DEFAULT_BUFFER_SIZE;

        /**
         * Sets the internal read buffer size in bytes.
         *
         * @param bufferSize buffer size in bytes (must be &gt; 0)
         * @return this builder
         */
        public Builder bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("bufferSize must be positive, got: " + bufferSize);
            }
            this.bufferSize = bufferSize;
            return this;
        }

        /** Builds the {@link DbfConfig}. */
        public DbfConfig build() {
            return new DbfConfig(this);
        }
    }
}
