package org.teacon.exhibition_portal.client.framework.binding;

public interface LayoutContext {
    <T> T get(LayoutBaseValue<T> parameter) throws LayoutFailureException;
}
