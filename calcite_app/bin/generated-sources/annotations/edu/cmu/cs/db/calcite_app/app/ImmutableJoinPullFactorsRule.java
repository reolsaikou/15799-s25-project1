package edu.cmu.cs.db.calcite_app.app;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.tools.RelBuilderFactory;
import org.immutables.value.Generated;

/**
 * {@code ImmutableJoinPullFactorsRule} contains immutable implementation classes generated from
 * abstract value types defined as nested inside {@link JoinPullFactorsRule}.
 * @see ImmutableJoinPullFactorsRule.Config
 */
@Generated(from = "JoinPullFactorsRule", generator = "Immutables")
@SuppressWarnings({"all"})
@ParametersAreNonnullByDefault
@javax.annotation.processing.Generated("org.immutables.processor.ProxyProcessor")
public final class ImmutableJoinPullFactorsRule {
  private ImmutableJoinPullFactorsRule() {}

  /**
   * Immutable implementation of {@link JoinPullFactorsRule.Config}.
   * <p>
   * Use the builder to create immutable instances:
   * {@code ImmutableJoinPullFactorsRule.Config.builder()}.
   * Use the static factory method to get the default singleton instance:
   * {@code ImmutableJoinPullFactorsRule.Config.of()}.
   */
  @Generated(from = "JoinPullFactorsRule.Config", generator = "Immutables")
  @Immutable
  @CheckReturnValue
  public static final class Config implements JoinPullFactorsRule.Config {
    private final RelBuilderFactory relBuilderFactory;
    private final @Nullable @org.checkerframework.checker.nullness.qual.Nullable String description;
    private final RelRule.OperandTransform operandSupplier;

    private Config() {
      this.description = null;
      this.relBuilderFactory = initShim.relBuilderFactory();
      this.operandSupplier = initShim.operandSupplier();
      this.initShim = null;
    }

    private Config(ImmutableJoinPullFactorsRule.Config.Builder builder) {
      this.description = builder.description;
      if (builder.relBuilderFactory != null) {
        initShim.relBuilderFactory(builder.relBuilderFactory);
      }
      if (builder.operandSupplier != null) {
        initShim.operandSupplier(builder.operandSupplier);
      }
      this.relBuilderFactory = initShim.relBuilderFactory();
      this.operandSupplier = initShim.operandSupplier();
      this.initShim = null;
    }

    private Config(
        RelBuilderFactory relBuilderFactory,
        @Nullable @org.checkerframework.checker.nullness.qual.Nullable String description,
        RelRule.OperandTransform operandSupplier) {
      this.relBuilderFactory = relBuilderFactory;
      this.description = description;
      this.operandSupplier = operandSupplier;
      this.initShim = null;
    }

    private static final byte STAGE_INITIALIZING = -1;
    private static final byte STAGE_UNINITIALIZED = 0;
    private static final byte STAGE_INITIALIZED = 1;
    @SuppressWarnings("Immutable")
    private transient volatile InitShim initShim = new InitShim();

    @Generated(from = "JoinPullFactorsRule.Config", generator = "Immutables")
    private final class InitShim {
      private byte relBuilderFactoryBuildStage = STAGE_UNINITIALIZED;
      private RelBuilderFactory relBuilderFactory;

      RelBuilderFactory relBuilderFactory() {
        if (relBuilderFactoryBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
        if (relBuilderFactoryBuildStage == STAGE_UNINITIALIZED) {
          relBuilderFactoryBuildStage = STAGE_INITIALIZING;
          RelBuilderFactory computedValue = relBuilderFactoryInitialize();
          this.relBuilderFactory = Objects.requireNonNull(computedValue, "relBuilderFactory");
          relBuilderFactoryBuildStage = STAGE_INITIALIZED;
        }
        return this.relBuilderFactory;
      }

      void relBuilderFactory(RelBuilderFactory relBuilderFactory) {
        this.relBuilderFactory = relBuilderFactory;
        relBuilderFactoryBuildStage = STAGE_INITIALIZED;
      }

      private byte operandSupplierBuildStage = STAGE_UNINITIALIZED;
      private RelRule.OperandTransform operandSupplier;

      RelRule.OperandTransform operandSupplier() {
        if (operandSupplierBuildStage == STAGE_INITIALIZING) throw new IllegalStateException(formatInitCycleMessage());
        if (operandSupplierBuildStage == STAGE_UNINITIALIZED) {
          operandSupplierBuildStage = STAGE_INITIALIZING;
          RelRule.OperandTransform computedValue = operandSupplierInitialize();
          this.operandSupplier = Objects.requireNonNull(computedValue, "operandSupplier");
          operandSupplierBuildStage = STAGE_INITIALIZED;
        }
        return this.operandSupplier;
      }

      void operandSupplier(RelRule.OperandTransform operandSupplier) {
        this.operandSupplier = operandSupplier;
        operandSupplierBuildStage = STAGE_INITIALIZED;
      }

      private String formatInitCycleMessage() {
        List<String> attributes = new ArrayList<>();
        if (relBuilderFactoryBuildStage == STAGE_INITIALIZING) attributes.add("relBuilderFactory");
        if (operandSupplierBuildStage == STAGE_INITIALIZING) attributes.add("operandSupplier");
        return "Cannot build Config, attribute initializers form cycle " + attributes;
      }
    }

    private RelBuilderFactory relBuilderFactoryInitialize() {
      return JoinPullFactorsRule.Config.super.relBuilderFactory();
    }

    private RelRule.OperandTransform operandSupplierInitialize() {
      return JoinPullFactorsRule.Config.super.operandSupplier();
    }

    /**
     * @return The value of the {@code relBuilderFactory} attribute
     */
    @Override
    public RelBuilderFactory relBuilderFactory() {
      InitShim shim = this.initShim;
      return shim != null
          ? shim.relBuilderFactory()
          : this.relBuilderFactory;
    }

    /**
     * @return The value of the {@code description} attribute
     */
    @Override
    public @Nullable @org.checkerframework.checker.nullness.qual.Nullable String description() {
      return description;
    }

    /**
     * @return The value of the {@code operandSupplier} attribute
     */
    @Override
    public RelRule.OperandTransform operandSupplier() {
      InitShim shim = this.initShim;
      return shim != null
          ? shim.operandSupplier()
          : this.operandSupplier;
    }

    /**
     * Copy the current immutable object by setting a value for the {@link JoinPullFactorsRule.Config#relBuilderFactory() relBuilderFactory} attribute.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     * @param value A new value for relBuilderFactory
     * @return A modified copy or the {@code this} object
     */
    public final ImmutableJoinPullFactorsRule.Config withRelBuilderFactory(RelBuilderFactory value) {
      if (this.relBuilderFactory == value) return this;
      RelBuilderFactory newValue = Objects.requireNonNull(value, "relBuilderFactory");
      return validate(new ImmutableJoinPullFactorsRule.Config(newValue, this.description, this.operandSupplier));
    }

    /**
     * Copy the current immutable object by setting a value for the {@link JoinPullFactorsRule.Config#description() description} attribute.
     * An equals check used to prevent copying of the same value by returning {@code this}.
     * @param value A new value for description (can be {@code null})
     * @return A modified copy or the {@code this} object
     */
    public final ImmutableJoinPullFactorsRule.Config withDescription(@Nullable @org.checkerframework.checker.nullness.qual.Nullable String value) {
      if (Objects.equals(this.description, value)) return this;
      return validate(new ImmutableJoinPullFactorsRule.Config(this.relBuilderFactory, value, this.operandSupplier));
    }

    /**
     * Copy the current immutable object by setting a value for the {@link JoinPullFactorsRule.Config#operandSupplier() operandSupplier} attribute.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     * @param value A new value for operandSupplier
     * @return A modified copy or the {@code this} object
     */
    public final ImmutableJoinPullFactorsRule.Config withOperandSupplier(RelRule.OperandTransform value) {
      if (this.operandSupplier == value) return this;
      RelRule.OperandTransform newValue = Objects.requireNonNull(value, "operandSupplier");
      return validate(new ImmutableJoinPullFactorsRule.Config(this.relBuilderFactory, this.description, newValue));
    }

    /**
     * This instance is equal to all instances of {@code Config} that have equal attribute values.
     * @return {@code true} if {@code this} is equal to {@code another} instance
     */
    @Override
    public boolean equals(@Nullable Object another) {
      if (this == another) return true;
      return another instanceof ImmutableJoinPullFactorsRule.Config
          && equalsByValue((ImmutableJoinPullFactorsRule.Config) another);
    }

    private boolean equalsByValue(ImmutableJoinPullFactorsRule.Config another) {
      return relBuilderFactory.equals(another.relBuilderFactory)
          && Objects.equals(description, another.description)
          && operandSupplier.equals(another.operandSupplier);
    }

    /**
     * Computes a hash code from attributes: {@code relBuilderFactory}, {@code description}, {@code operandSupplier}.
     * @return hashCode value
     */
    @Override
    public int hashCode() {
      @Var int h = 5381;
      h += (h << 5) + relBuilderFactory.hashCode();
      h += (h << 5) + Objects.hashCode(description);
      h += (h << 5) + operandSupplier.hashCode();
      return h;
    }

    /**
     * Prints the immutable value {@code Config} with attribute values.
     * @return A string representation of the value
     */
    @Override
    public String toString() {
      return MoreObjects.toStringHelper("Config")
          .omitNullValues()
          .add("relBuilderFactory", relBuilderFactory)
          .add("description", description)
          .add("operandSupplier", operandSupplier)
          .toString();
    }

    private static final ImmutableJoinPullFactorsRule.Config INSTANCE = validate(new ImmutableJoinPullFactorsRule.Config());

    /**
     * Returns the default immutable singleton value of {@code Config}
     * @return An immutable instance of Config
     */
    public static ImmutableJoinPullFactorsRule.Config of() {
      return INSTANCE;
    }

    private static ImmutableJoinPullFactorsRule.Config validate(ImmutableJoinPullFactorsRule.Config instance) {
      return INSTANCE != null && INSTANCE.equalsByValue(instance) ? INSTANCE : instance;
    }

    /**
     * Creates an immutable copy of a {@link JoinPullFactorsRule.Config} value.
     * Uses accessors to get values to initialize the new immutable instance.
     * If an instance is already immutable, it is returned as is.
     * @param instance The instance to copy
     * @return A copied immutable Config instance
     */
    public static ImmutableJoinPullFactorsRule.Config copyOf(JoinPullFactorsRule.Config instance) {
      if (instance instanceof ImmutableJoinPullFactorsRule.Config) {
        return (ImmutableJoinPullFactorsRule.Config) instance;
      }
      return ImmutableJoinPullFactorsRule.Config.builder()
          .from(instance)
          .build();
    }

    /**
     * Creates a builder for {@link ImmutableJoinPullFactorsRule.Config Config}.
     * <pre>
     * ImmutableJoinPullFactorsRule.Config.builder()
     *    .relBuilderFactory(org.apache.calcite.tools.RelBuilderFactory) // optional {@link JoinPullFactorsRule.Config#relBuilderFactory() relBuilderFactory}
     *    .description(String | null) // nullable {@link JoinPullFactorsRule.Config#description() description}
     *    .operandSupplier(org.apache.calcite.plan.RelRule.OperandTransform) // optional {@link JoinPullFactorsRule.Config#operandSupplier() operandSupplier}
     *    .build();
     * </pre>
     * @return A new Config builder
     */
    public static ImmutableJoinPullFactorsRule.Config.Builder builder() {
      return new ImmutableJoinPullFactorsRule.Config.Builder();
    }

    /**
     * Builds instances of type {@link ImmutableJoinPullFactorsRule.Config Config}.
     * Initialize attributes and then invoke the {@link #build()} method to create an
     * immutable instance.
     * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
     * but instead used immediately to create instances.</em>
     */
    @Generated(from = "JoinPullFactorsRule.Config", generator = "Immutables")
    public static final class Builder {
      private @Nullable RelBuilderFactory relBuilderFactory;
      private @Nullable @org.checkerframework.checker.nullness.qual.Nullable String description;
      private @Nullable RelRule.OperandTransform operandSupplier;

      private Builder() {
      }

      /**
       * Fill a builder with attribute values from the provided {@code edu.cmu.cs.db.calcite_app.app.JoinPullFactorsRule.Config} instance.
       * @param instance The instance from which to copy values
       * @return {@code this} builder for use in a chained invocation
       */
      @CanIgnoreReturnValue 
      public final Builder from(JoinPullFactorsRule.Config instance) {
        Objects.requireNonNull(instance, "instance");
        mergeInternal(instance);
        return this;
      }

      /**
       * Fill a builder with attribute values from the provided {@code org.apache.calcite.plan.RelRule.Config} instance.
       * @param instance The instance from which to copy values
       * @return {@code this} builder for use in a chained invocation
       */
      @CanIgnoreReturnValue 
      public final Builder from(RelRule.Config instance) {
        Objects.requireNonNull(instance, "instance");
        mergeInternal(instance);
        return this;
      }

      private void mergeInternal(Object object) {
        @Var long bits = 0;
        if (object instanceof JoinPullFactorsRule.Config) {
          JoinPullFactorsRule.Config instance = (JoinPullFactorsRule.Config) object;
          if ((bits & 0x2L) == 0) {
            this.relBuilderFactory(instance.relBuilderFactory());
            bits |= 0x2L;
          }
          if ((bits & 0x1L) == 0) {
            @Nullable @org.checkerframework.checker.nullness.qual.Nullable String descriptionValue = instance.description();
            if (descriptionValue != null) {
              description(descriptionValue);
            }
            bits |= 0x1L;
          }
          if ((bits & 0x4L) == 0) {
            this.operandSupplier(instance.operandSupplier());
            bits |= 0x4L;
          }
        }
        if (object instanceof RelRule.Config) {
          RelRule.Config instance = (RelRule.Config) object;
          if ((bits & 0x2L) == 0) {
            this.relBuilderFactory(instance.relBuilderFactory());
            bits |= 0x2L;
          }
          if ((bits & 0x1L) == 0) {
            @Nullable @org.checkerframework.checker.nullness.qual.Nullable String descriptionValue = instance.description();
            if (descriptionValue != null) {
              description(descriptionValue);
            }
            bits |= 0x1L;
          }
          if ((bits & 0x4L) == 0) {
            this.operandSupplier(instance.operandSupplier());
            bits |= 0x4L;
          }
        }
      }

      /**
       * Initializes the value for the {@link JoinPullFactorsRule.Config#relBuilderFactory() relBuilderFactory} attribute.
       * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link JoinPullFactorsRule.Config#relBuilderFactory() relBuilderFactory}.</em>
       * @param relBuilderFactory The value for relBuilderFactory 
       * @return {@code this} builder for use in a chained invocation
       */
      @CanIgnoreReturnValue 
      public final Builder relBuilderFactory(RelBuilderFactory relBuilderFactory) {
        this.relBuilderFactory = Objects.requireNonNull(relBuilderFactory, "relBuilderFactory");
        return this;
      }

      /**
       * Initializes the value for the {@link JoinPullFactorsRule.Config#description() description} attribute.
       * @param description The value for description (can be {@code null})
       * @return {@code this} builder for use in a chained invocation
       */
      @CanIgnoreReturnValue 
      public final Builder description(@Nullable @org.checkerframework.checker.nullness.qual.Nullable String description) {
        this.description = description;
        return this;
      }

      /**
       * Initializes the value for the {@link JoinPullFactorsRule.Config#operandSupplier() operandSupplier} attribute.
       * <p><em>If not set, this attribute will have a default value as returned by the initializer of {@link JoinPullFactorsRule.Config#operandSupplier() operandSupplier}.</em>
       * @param operandSupplier The value for operandSupplier 
       * @return {@code this} builder for use in a chained invocation
       */
      @CanIgnoreReturnValue 
      public final Builder operandSupplier(RelRule.OperandTransform operandSupplier) {
        this.operandSupplier = Objects.requireNonNull(operandSupplier, "operandSupplier");
        return this;
      }

      /**
       * Builds a new {@link ImmutableJoinPullFactorsRule.Config Config}.
       * @return An immutable instance of Config
       * @throws java.lang.IllegalStateException if any required attributes are missing
       */
      public ImmutableJoinPullFactorsRule.Config build() {
        return ImmutableJoinPullFactorsRule.Config.validate(new ImmutableJoinPullFactorsRule.Config(this));
      }
    }
  }
}
