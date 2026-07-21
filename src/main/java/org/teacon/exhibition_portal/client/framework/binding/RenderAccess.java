package org.teacon.exhibition_portal.client.framework.binding;

public final class RenderAccess {
    private RenderAccess() {
    }

    static final ScopedValue<LayoutBaseValue<?>> pending = ScopedValue.newInstance();

    public static <T> T get(LayoutParameter<T> parameter) {
        assertState();
        return parameter.getValue();
    }

    public static <T> T get(LayoutBinding<T> parameter) throws LayoutFailureException {
        assertState();
        return parameter.getValue();
    }

    public static <T> T get(LayoutBinding<T> parameter, T defaultValue) {
        try {
            return get(parameter);
        } catch (LayoutFailureException _) {
            return defaultValue;
        }
    }

    /* package-private */ static <T> void assertState() {
        if (pending.isBound()) {
            throw new IllegalArgumentException("Cannot get value while layout.");
        }
    }
}
