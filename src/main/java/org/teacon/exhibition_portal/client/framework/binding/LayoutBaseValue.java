package org.teacon.exhibition_portal.client.framework.binding;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public abstract sealed class LayoutBaseValue<T> permits LayoutBinding, LayoutParameter {
    protected final DeclarationSource declaration;
    protected final Set<LayoutBinding<?>> downstream = Collections.newSetFromMap(new WeakHashMap<>());

    protected LayoutBaseValue(DeclarationSource declaration) {
        this.declaration = declaration;
    }

    protected void populateChange() {
        for (LayoutBinding<?> binding : downstream) {
            binding.markDirty();
        }
    }

    abstract T getValue() throws LayoutFailureException;

    public record DeclarationSource(Class<?> clazz, String method, int line) {
        private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

        public static DeclarationSource capture() {
            StackWalker.StackFrame caller = WALKER.walk(stream -> stream
                    .skip(2)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Cannot locate layout's declaration."))
            );

            return new DeclarationSource(caller.getDeclaringClass(), caller.getMethodName(), caller.getLineNumber());
        }
    }
}
