package io.github.railroad.settings;

import com.google.gson.JsonElement;
import javafx.scene.Node;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a setting in the Railroad plugin system.
 * Each setting has an ID, a tree path, a codec for serialization/deserialization,
 * a type, a nullability flag, and a default value.
 * It also supports listeners that can be notified when the setting's value changes.
 *
 * @param <T> The type of the setting's value.
 */
public class Setting<T> {
    /**
     * Unique identifier for the setting.
     * This ID is used to reference the setting in the settings system.
     */
    @Getter
    private final String id;

    /**
     * The tree path for the setting, used for organizing settings in a hierarchical structure.
     * This path is typically used in UI representations or configuration files.
     */
    @Getter
    private final String treePath;

    /**
     * The codec used for serializing and deserializing the setting's value.
     * This allows the setting to be stored in a persistent format (e.g., JSON).
     */
    @Getter
    private final SettingCodec<T, ?> codec;

    /**
     * The type of the setting's value.
     * This is used to enforce type safety when setting or getting the value.
     */
    @Getter
    private final Class<T> type;

    /**
     * Indicates whether the setting's value can be null.
     * If true, the setting can have a null value; otherwise, it must always have a non-null value.
     */
    @Getter
    private final boolean canBeNull;

    /**
     * The default value for the setting.
     * This value is used when the setting is first created or reset.
     * It can be null if the setting allows null values.
     */
    @Getter
    private final T defaultValue;

    /**
     * List of listeners that are notified when the setting's value changes.
     * This allows other parts of the application to react to changes in the setting.
     */
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    /**
     * The current value of the setting.
     * This is the value that is actively used by the application.
     * It can be null if the setting allows null values.
     */
    @Nullable
    @Getter
    private T value;

    /**
     * Constructs a new Setting builder.
     *
     * @return A new Builder instance for creating a Setting.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Constructs a new Setting builder with the specified type.
     *
     * @param type The class type of the setting's value.
     * @return A new Builder instance for creating a Setting with the specified type.
     */
    public static <T> Builder<T> builder(Class<T> type) {
        return Setting.<T>builder().type(type);
    }

    /**
     * Constructs a new Setting builder with the specified type and ID.
     *
     * @param type The class type of the setting's value.
     * @param id   The unique identifier for the setting.
     * @return A new Builder instance for creating a Setting with the specified type and ID.
     */
    public static <T> Builder<T> builder(Class<T> type, String id) {
        return builder(type).id(id);
    }

    /**
     * Constructs a new Setting instance.
     *
     * @param id           Unique identifier for the setting.
     * @param treePath     The tree path for the setting.
     * @param codec        The codec used for serialization/deserialization.
     * @param type         The type of the setting's value.
     * @param canBeNull    Indicates if the setting's value can be null.
     * @param defaultValue The default value for the setting, can be null if allowed.
     */
    public Setting(String id, String treePath, SettingCodec<T, ?> codec, Class<T> type, boolean canBeNull, @Nullable T defaultValue) {
        this.id = id;
        this.treePath = treePath;
        this.codec = codec;
        this.type = type;
        this.canBeNull = canBeNull;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /**
     * Constructs a new Setting instance with a default value of null.
     * This is a convenience constructor for settings that do not require a default value.
     *
     * @param id       Unique identifier for the setting.
     * @param treePath The tree path for the setting.
     * @param codec    The codec used for serialization/deserialization.
     * @param type     The type of the setting's value.
     */
    public Setting(String id, String treePath, SettingCodec<T, ?> codec, Class<T> type) {
        this(id, treePath, codec, type, false, null);
    }

    /**
     * Gets the current value of the setting.
     *
     * @return The current value of the setting, or the default value if the current value is null and cannot be null.
     */
    public T getOrDefaultValue() {
        if (value == null)
            return defaultValue;

        return value;
    }

    /**
     * Gets the current value of the setting wrapped in an Optional.
     * If the value is null and the setting does not allow nulls, it returns an empty Optional.
     *
     * @return An Optional containing the current value of the setting, or empty if the value is null and cannot be null.
     */
    public Optional<T> getOptional() {
        return canBeNull ? Optional.ofNullable(value) : Optional.ofNullable(getOrDefaultValue());
    }

    /**
     * Gets the current value of the setting, or throws an exception if the value is null.
     * This method allows for custom exception handling by providing a Supplier for the exception.
     *
     * @param exceptionSupplier A Supplier that provides the exception to throw if the value is null.
     * @return The current value of the setting.
     * @throws Throwable If the value is null and cannot be null, or if the exception supplier throws an exception.
     */
    public T getOrThrow(Supplier<Throwable> exceptionSupplier) throws Throwable {
        return getOptional().orElseThrow(exceptionSupplier);
    }

    /**
     * Gets the current value of the setting, or throws an IllegalStateException with a custom message if the value is null.
     *
     * @param message The message to include in the exception if the value is null.
     * @return The current value of the setting.
     * @throws Throwable If the value is null and cannot be null, or if the exception supplier throws an exception.
     */
    public T getOrThrow(String message) throws Throwable {
        return getOptional().orElseThrow(() -> new IllegalStateException(message));
    }

    /**
     * Gets the current value of the setting, or throws an IllegalStateException if the value is null.
     * This method is a convenience method that does not require a custom message.
     *
     * @return The current value of the setting.
     * @throws Throwable If the value is null and cannot be null.
     */
    public T getOrThrow() throws Throwable {
        return getOptional().orElseThrow(() -> new IllegalStateException("Setting value is null and cannot be null"));
    }

    /**
     * Gets the current value of the setting.
     * If the value is null and the setting does not allow nulls, it returns the default value.
     */
    public void setValue(T value) {
        if (value == null && !canBeNull)
            throw new IllegalArgumentException("Setting value cannot be null");

        this.value = value;
        notifyListeners();
    }

    /**
     * Forces an update of the setting's value, notifying all listeners.
     * This method is typically used when the value is set externally or needs to be refreshed.
     */
    public void forceUpdate() {
        setValue(this.value);
    }

    /**
     * Gets the current value of the setting.
     * If the value is null and the setting does not allow nulls, it returns the default value.
     */
    public void addListener(@NotNull Consumer<T> listener) {
        if (listener == null)
            throw new IllegalArgumentException("Listener cannot be null");

        this.listeners.add(listener);
    }

    /**
     * Removes a listener from the setting.
     * The listener will no longer be notified when the setting's value changes.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(@NotNull Consumer<T> listener) {
        if (listener == null)
            throw new IllegalArgumentException("Listener cannot be null");

        this.listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners about the change in the setting's value.
     * This method is called whenever the value is set or changed.
     */
    private void notifyListeners() {
        for (Consumer<T> listener : this.listeners) {
            listener.accept(this.value);
        }
    }

    /**
     * Serializes the setting's value to a JSON element using the codec.
     * If the codec is not set, it logs a warning and returns null.
     *
     * @return A JsonElement representing the setting's value, or null if the codec is not set.
     */
    public @Nullable JsonElement toJson() {
        if (this.codec == null)
            return null;

        return this.codec.serializeJson(this.value);
    }

    /**
     * Deserializes the setting's value from a JSON element.
     * If the JSON element is null, it sets the value to the default value if allowed.
     * If the codec is not set, it throws an IllegalStateException.
     *
     * @param json The JSON element to deserialize from, can be null.
     * @return The deserialized value of the setting.
     * @throws IllegalStateException If the codec is null or if deserialization fails.
     */
    public T fromJson(@Nullable JsonElement json) throws IllegalStateException {
        if (this.codec == null)
            throw new IllegalStateException();

        if (json == null) {
            if (this.canBeNull) {
                this.value = null;
            } else {
                this.value = this.defaultValue;
            }
        } else {
            this.value = this.codec.deserializeJson(json);
        }

        return this.value;
    }

    /**
     * Updates the setting's value from a JSON element and notifies listeners.
     * This method is typically used when the setting is updated in a UI or configuration file.
     *
     * @param json The JSON element to update the setting from, can be null.
     * @return The updated value of the setting.
     * @throws IllegalStateException If the codec is null or if deserialization fails.
     */
    public T fromJsonUpdate(@Nullable JsonElement json) throws IllegalStateException {
        T value = fromJson(json);
        notifyListeners();
        return value;
    }

    /**
     * Reads the value from a Node using the codec.
     * If the codec is not set, it returns null.
     * If the node cannot be converted to the expected type, it throws a ClassCastException.
     *
     * @param node The Node to read the value from.
     * @return The value read from the Node, or null if the codec is not set.
     * @throws IllegalStateException If the codec does not support reading from nodes.
     */
    public <N extends Node> T readValueFromNode(N node) {
        if (this.codec == null)
            return null;

        try {
            @SuppressWarnings("unchecked")
            Function<N, T> nodeToValue = (Function<N, T>) codec.nodeToValue();
            T value = nodeToValue.apply(node);
            setValue(value);
            return value;
        } catch (ClassCastException exception) {
            throw new IllegalStateException("Codec for setting '" + this.id + "' does not support reading from node", exception);
        }
    }

    /**
     * Creates a Node representation of the setting's value using the codec.
     * If the codec is not set, it returns null.
     *
     * @return A Node representing the setting's value, or null if the codec is not set.
     */
    public Node createNode() {
        if (this.codec == null)
            return null;

        return codec.createNode().apply(this.value);
    }

    /**
     * Builder class for creating instances of Setting.
     *
     * @param <T> The type of the setting's value.
     */
    public static class Builder<T> {
        private String id = "undefined";
        private String treePath = "undefined";
        private SettingCodec<T, ?> codec;
        private Class<T> type;
        private boolean canBeNull = false;
        private T defaultValue;
        private final List<Consumer<T>> listeners = new ArrayList<>();

        /**
         * Sets the unique identifier for the setting.
         *
         * @param id The unique identifier for the setting.
         * @return This builder instance for method chaining.
         */
        public Builder<T> id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the tree path for the setting.
         *
         * @param treePath The tree path for the setting.
         * @return This builder instance for method chaining.
         */
        public Builder<T> treePath(String treePath) {
            this.treePath = treePath;
            return this;
        }

        /**
         * Sets the codec used for serializing and deserializing the setting's value.
         *
         * @param codec The codec for the setting.
         * @return This builder instance for method chaining.
         */
        public Builder<T> codec(SettingCodec<T, ?> codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Sets the type of the setting's value.
         *
         * @param type The class type of the setting's value.
         * @return This builder instance for method chaining.
         */
        public Builder<T> type(Class<T> type) {
            this.type = type;
            return this;
        }

        /**
         * Sets whether the setting's value can be null.
         *
         * @param canBeNull True if the setting can have a null value, false otherwise.
         * @return This builder instance for method chaining.
         */
        public Builder<T> canBeNull(boolean canBeNull) {
            this.canBeNull = canBeNull;
            return this;
        }

        /**
         * Sets the default value for the setting.
         *
         * @param defaultValue The default value for the setting, can be null if allowed.
         * @return This builder instance for method chaining.
         */
        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Adds a listener that will be notified when the setting's value changes.
         *
         * @param listener The listener to add.
         * @return This builder instance for method chaining.
         */
        public Builder<T> addListener(@NotNull Consumer<T> listener) {
            if (listener == null)
                throw new IllegalArgumentException("Listener cannot be null");

            this.listeners.add(listener);
            return this;
        }

        /**
         * Builds the Setting instance with the provided parameters.
         *
         * @return A new Setting instance.
         * @throws IllegalStateException If any required fields are missing.
         */
        public Setting<T> build() {
            if (id == null || treePath == null || codec == null || type == null)
                throw new IllegalStateException("Setting is missing required fields");

            var setting = new Setting<>(id, treePath, codec, type, canBeNull, defaultValue);
            for (Consumer<T> listener : listeners) {
                setting.addListener(listener);
            }

            return setting;
        }
    }
}
