package org.teacon.exhibition_portal.client.framework.binding;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.neoforged.neoforge.common.util.strategy.IdentityStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public non-sealed class LayoutBinding<T> extends LayoutBaseValue<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayoutBinding.class);

    @FunctionalInterface
    public interface LayoutFunction<T> {
        T layout(LayoutContext context) throws LayoutFailureException;
    }

    public static <T> LayoutBinding<T> of(Supplier<List<LayoutBaseValue<?>>> upstream, LayoutFunction<T> factory) {
        return new LayoutBinding<>(DeclarationSource.capture(), factory, upstream);
    }

    private sealed interface State<T> {
        record Uninitialized<T>(Supplier<List<LayoutBaseValue<?>>> upstream) implements State<T> {
        }

        record Initializing<T>() implements State<T> {
        }

        record Dirty<T>(boolean success, T previous) implements State<T> {
        }

        record Pending<T>() implements State<T> {
        }

        record Ready<T>(T value) implements State<T> {
        }

        record Failed<T>(LayoutFailureException exception) implements State<T> {
        }

        default String type() {
            return this.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        }
    }

    private State<T> state;

    private final LayoutFunction<T> factory;

    private final ObjectSet<LayoutBaseValue<?>> resolvedUpstream = new ObjectOpenCustomHashSet<>(IdentityStrategy.IDENTITY);

    private final LayoutContext layoutContext = new LayoutContext() {
        @Override
        public <S> S get(LayoutBaseValue<S> upstream) throws LayoutFailureException {
            if (!(LayoutBinding.this.state instanceof State.Pending<T>)) {
                throw new LayoutFailureException("Cannot access dependency: current node isn't pending")
                        .pushSource(LayoutFailureException.Operation.ACCESS_DEPENDENCY, upstream);
            }

            if (!LayoutBinding.this.resolvedUpstream.contains(upstream)) {
                throw new LayoutFailureException("Cannot access dependency: unregistered dependency")
                        .pushSource(LayoutFailureException.Operation.ACCESS_DEPENDENCY, upstream);
            }
            return upstream.getValue();
        }
    };

    protected LayoutBinding(DeclarationSource source, LayoutFunction<T> factory, Supplier<List<LayoutBaseValue<?>>> upstream) {
        super(source);
        this.factory = factory;
        this.state = new State.Uninitialized<>(upstream);
    }

    void markDirty() {
        switch (state) {
            case State.Uninitialized<T> _, State.Initializing<T> _ ->
                    throw new AssertionError("Cannot mark as dirty: " + this.state.type());
            case State.Dirty<T> _, State.Pending<T> _ -> {
            }
            case State.Ready<T>(T value) -> {
                state = new State.Dirty<>(true, value);
                populateChange();
            }
            case State.Failed<T> _ -> {
                state = new State.Dirty<>(false, null);
                populateChange();
            }
        }
    }

    T getValue() throws LayoutFailureException {
        switch (state) {
            case State.Initializing<T> _, State.Pending<T> _ ->
                    throw new AssertionError("Cannot get value: " + this.state.type());
            case State.Uninitialized<T>(Supplier<List<LayoutBaseValue<?>>> upstreamSupplier) -> {
                List<LayoutBaseValue<?>> upstream = upstreamSupplier.get();
                for (LayoutBaseValue<?> binding : upstream) {
                    Objects.requireNonNull(binding);
                }
                resolvedUpstream.addAll(upstream);
                for (LayoutBaseValue<?> binding : upstream) {
                    binding.downstream.add(this);
                }

                state = new State.Pending<>();
                try {
                    T value = computeValue();
                    state = new State.Ready<>(value);
                    return value;
                } catch (LayoutFailureException e) {
                    state = new State.Failed<>(e);
                    throw e;
                }
            }
            case State.Dirty<T>(boolean success, T previous) -> {
                state = new State.Pending<>();
                try {
                    T value = computeValue();
                    if (!success || !Objects.equals(previous, value)) {
                        populateChange();
                    }
                    state = new State.Ready<>(value);
                    return value;
                } catch (LayoutFailureException e) {
                    state = new State.Failed<>(e);
                    if (success) {
                        populateChange();
                    }
                    throw e;
                }
            }
            case State.Ready<T>(T value) -> {
                return value;
            }
            case State.Failed<T>(LayoutFailureException exception) -> throw exception;
        }
    }

    private T computeValue() throws LayoutFailureException {
        try {
            try {
                return ScopedValue.where(RenderAccess.pending, this)
                        .call(() -> factory.layout(layoutContext));
            } catch (RuntimeException e) {
                throw new LayoutFailureException(e);
            }
        } catch (LayoutFailureException e) {
            e.pushSource(LayoutFailureException.Operation.COMPUTING_VALUE, this);
            if (!RenderAccess.pending.isBound()) {
                LOGGER.warn("Cannot layout UI.", e);
            }
            throw e;
        }
    }
}
