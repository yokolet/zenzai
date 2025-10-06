package nokogiri.internals.html.internal;

import java.io.IOException;

public abstract class QuietAppendable {
    public abstract QuietAppendable append(CharSequence csq);

    public abstract QuietAppendable append(char c);

    public abstract QuietAppendable append(char[] chars, int offset, int len); // via StringBuilder, not Appendable

    public static QuietAppendable wrap(Appendable a) {
        if (a instanceof StringBuilder) return new StringBuilderAppendable((StringBuilder) a);
        else                            return new BaseAppendable(a);
    }

    static final class BaseAppendable extends QuietAppendable {
        private final Appendable a;

        @FunctionalInterface
        private interface Action {
            void append() throws IOException;
        }

        private BaseAppendable(Appendable appendable) {
            this.a = appendable;
        }

        private BaseAppendable quiet(Action action) {
            try {
                action.append();
            } catch (IOException e) {
                throw new SerializationException(e);
            }
            return this;
        }

        @Override
        public BaseAppendable append(CharSequence csq) {
            return quiet(() -> a.append(csq));
        }

        @Override
        public BaseAppendable append(char c) {
            return quiet(() -> a.append(c));
        }

        @Override
        public QuietAppendable append(char[] chars, int offset, int len) {
            return quiet(() -> a.append(new String(chars, offset, len)));
        }
    }

    /** A version that wraps a StringBuilder, and so doesn't need the exception wrap. */
    static final class StringBuilderAppendable extends QuietAppendable {
        private final StringBuilder sb;

        private StringBuilderAppendable(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        public StringBuilderAppendable append(CharSequence csq) {
            sb.append(csq);
            return this;
        }

        @Override
        public StringBuilderAppendable append(char c) {
            sb.append(c);
            return this;
        }

        @Override
        public QuietAppendable append(char[] chars, int offset, int len) {
            sb.append(chars, offset, len);
            return this;
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    static final class SerializationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private SerializationException(Throwable cause) {
            super(cause);
        }
    }
}
