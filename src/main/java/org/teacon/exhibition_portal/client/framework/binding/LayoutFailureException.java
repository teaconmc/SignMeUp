package org.teacon.exhibition_portal.client.framework.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LayoutFailureException extends Exception {
    public LayoutFailureException(String message) {
        super(message);
    }

    public LayoutFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public LayoutFailureException(Throwable cause) {
        super(cause);
    }

    public enum Operation {
        RENDER_ACCESS,
        ACCESS_DEPENDENCY,
        COMPUTING_VALUE
    }

    private final List<Source> sources = new ArrayList<>();

    public record Source(Operation operation, LayoutBaseValue.DeclarationSource declaration) {
    }

    public LayoutFailureException pushSource(Operation operation, LayoutBaseValue<?> target) {
        this.sources.add(new Source(operation, target.declaration));
        return this;
    }

    @Override
    public String getMessage() {
        String msg = super.getMessage();
        if (!this.sources.isEmpty()) {
            StringBuilder message = new StringBuilder(msg);
            for (Source source : this.sources) {
                message.append("\n\tat UI Layout/")
                        .append(source.operation().name().toLowerCase(Locale.ROOT))
                        .append('/')
                        .append(source.declaration().clazz().getName())
                        .append('.')
                        .append(source.declaration().method())
                        .append('(')
                        .append(source.declaration().clazz().getSimpleName())
                        .append(".java:")
                        .append(source.declaration().line())
                        .append(')');
            }
            return message.toString();
        }
        return msg;
    }
}
