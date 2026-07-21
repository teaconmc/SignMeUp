package org.teacon.exhibition_portal.client.framework.binding;

import java.util.Objects;

public non-sealed class LayoutParameter<T> extends LayoutBaseValue<T> {
    private T value;

    private LayoutParameter(DeclarationSource source, T value) {
        super(source);
        this.value = value;
    }

    T getValue() {
        return this.value;
    }

    public void set(T value) {
        RenderAccess.assertState();
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            this.populateChange();
        }
    }

    public void markDirty() {
        RenderAccess.assertState();
        this.populateChange();
    }

    public static <T> LayoutParameter<T> of(T value) {
        return new LayoutParameter<>(DeclarationSource.capture(), value);
    }
}
