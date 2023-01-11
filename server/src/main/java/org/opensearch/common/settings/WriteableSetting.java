/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.common.settings;

import org.opensearch.Version;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.unit.ByteSizeValue;
import org.opensearch.common.unit.TimeValue;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for {@link Setting} with {@link #writeTo(StreamOutput)} implementation dependent on the setting type.
 *
 * @opensearch.internal
 */
public class WriteableSetting implements Writeable {

    /**
     * The Generic Types which this class can serialize.
     */
    public enum SettingType {
        Boolean,
        Integer,
        Long,
        Float,
        Double,
        String,
        TimeValue, // long + TimeUnit
        ByteSizeValue, // long + ByteSizeUnit
        Version
    }

    private Setting<?> setting;
    private SettingType type;

    /**
     * Wrap a {@link Setting}. The generic type is determined from the type of the default value.
     *
     * @param setting  The setting to wrap. The default value must not be null.
     * @throws IllegalArgumentException if the setting has a null default value.
     */
    public WriteableSetting(Setting<?> setting) {
        this(setting, getGenericTypeFromDefault(setting));
    }

    /**
     * Wrap a {@link Setting} with a specified generic type.
     *
     * @param setting  The setting to wrap.
     * @param type  The Generic type of the setting.
     */
    public WriteableSetting(Setting<?> setting, SettingType type) {
        this.setting = setting;
        this.type = type;
    }

    /**
     * Wrap a {@link Setting} read from a stream.
     *
     * @param in Input to read the value from.
     * @throws IOException if there is an error reading the values
     */
    public WriteableSetting(StreamInput in) throws IOException {
        // Read the type
        this.type = in.readEnum(SettingType.class);
        // Read the key
        String key = in.readString();
        // Read the default value
        Object defaultValue = readDefaultValue(in);
        // Read a boolean specifying whether the fallback settings is null
        WriteableSetting fallback = null;
        boolean hasFallback = in.readBoolean();
        if (hasFallback) {
            fallback = new WriteableSetting(in);
        }
        // We are using known types so don't need the parser
        // We are not using validator
        // Read properties
        EnumSet<Property> propSet = in.readEnumSet(Property.class);
        // Put it all in a setting object
        this.setting = createSetting(type, key, defaultValue, fallback, propSet.toArray(Property[]::new));
    }

    /**
     * Due to type erasure, it is impossible to determine the generic type of a Setting at runtime.
     * All settings have a non-null default, however, so the type of the default can be used to determine the setting type.
     *
     * @param setting The setting with a generic type.
     * @return The corresponding {@link SettingType} for the default value.
     */
    private static SettingType getGenericTypeFromDefault(Setting<?> setting) {
        String typeStr = setting.getDefault(Settings.EMPTY).getClass().getSimpleName();
        try {
            // This throws IAE if not in enum
            return SettingType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "This class is not yet set up to handle the generic type: "
                    + typeStr
                    + ". Supported types are "
                    + Arrays.toString(SettingType.values())
            );
        }
    }

    /**
     * Gets the wrapped setting. Use {@link #getType()} to determine its generic type.
     *
     * @return The wrapped setting.
     */
    public Setting<?> getSetting() {
        return this.setting;
    }

    /**
     * Gets the generic type of the wrapped setting.
     *
     * @return The wrapped setting's generic type.
     */
    public SettingType getType() {
        return this.type;
    }

    @SuppressWarnings("unchecked")
    private Setting<?> createSetting(
        SettingType type,
        String key,
        Object defaultValue,
        WriteableSetting fallback,
        Property[] propertyArray
    ) {
        switch (type) {
            case Boolean:
                return fallback == null
                    ? Setting.boolSetting(key, (boolean) defaultValue, propertyArray)
                    : Setting.boolSetting(key, (Setting<Boolean>) fallback.getSetting(), propertyArray);
            case Integer:
                return fallback == null
                    ? Setting.intSetting(key, (int) defaultValue, propertyArray)
                    : Setting.intSetting(key, (Setting<Integer>) fallback.getSetting(), propertyArray);
            case Long:
                return fallback == null
                    ? Setting.longSetting(key, (long) defaultValue, propertyArray)
                    : Setting.longSetting(key, (Setting<Long>) fallback.getSetting(), propertyArray);
            case Float:
                return fallback == null
                    ? Setting.floatSetting(key, (float) defaultValue, propertyArray)
                    : Setting.floatSetting(key, (Setting<Float>) fallback.getSetting(), propertyArray);
            case Double:
                return fallback == null
                    ? Setting.doubleSetting(key, (double) defaultValue, propertyArray)
                    : Setting.doubleSetting(key, (Setting<Double>) fallback.getSetting(), propertyArray);
            case String:
                return fallback == null
                    ? Setting.simpleString(key, (String) defaultValue, propertyArray)
                    : Setting.simpleString(key, (Setting<String>) fallback.getSetting(), propertyArray);
            case TimeValue:
                return fallback == null
                    ? Setting.timeSetting(key, (TimeValue) defaultValue, propertyArray)
                    : Setting.timeSetting(key, (Setting<TimeValue>) fallback.getSetting(), propertyArray);
            case ByteSizeValue:
                return fallback == null
                    ? Setting.byteSizeSetting(key, (ByteSizeValue) defaultValue, propertyArray)
                    : Setting.byteSizeSetting(key, (Setting<ByteSizeValue>) fallback.getSetting(), propertyArray);
            case Version:
                // No fallback option on this method
                return Setting.versionSetting(key, (Version) defaultValue, propertyArray);
            default:
                // This Should Never Happen (TM)
                throw new IllegalArgumentException("A SettingType has been added to the enum and not handled here.");
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        // Write the type
        out.writeEnum(type);
        // Write the key
        out.writeString(setting.getKey());
        // Write the default value
        writeDefaultValue(out, setting.getDefault(Settings.EMPTY));
        // Write a boolean specifying whether the fallback settings is null
        boolean hasFallback = setting.fallbackSetting != null;
        out.writeBoolean(hasFallback);
        if (hasFallback) {
            new WriteableSetting(setting.fallbackSetting, type).writeTo(out);
        }
        // We are using known types so don't need the parser
        // We are not using validator
        // Write properties
        out.writeEnumSet(setting.getProperties());
    }

    private void writeDefaultValue(StreamOutput out, Object defaultValue) throws IOException {
        switch (type) {
            case Boolean:
                out.writeBoolean((boolean) defaultValue);
                break;
            case Integer:
                out.writeInt((int) defaultValue);
                break;
            case Long:
                out.writeLong((long) defaultValue);
                break;
            case Float:
                out.writeFloat((float) defaultValue);
                break;
            case Double:
                out.writeDouble((double) defaultValue);
                break;
            case String:
                out.writeString((String) defaultValue);
                break;
            case TimeValue:
                TimeValue tv = (TimeValue) defaultValue;
                out.writeLong(tv.duration());
                out.writeString(tv.timeUnit().name());
                break;
            case ByteSizeValue:
                ((ByteSizeValue) defaultValue).writeTo(out);
                break;
            case Version:
                Version.writeVersion((Version) defaultValue, out);
                break;
            default:
                // This Should Never Happen (TM)
                throw new IllegalArgumentException("A SettingType has been added to the enum and not handled here.");
        }
    }

    private Object readDefaultValue(StreamInput in) throws IOException {
        switch (type) {
            case Boolean:
                return in.readBoolean();
            case Integer:
                return in.readInt();
            case Long:
                return in.readLong();
            case Float:
                return in.readFloat();
            case Double:
                return in.readDouble();
            case String:
                return in.readString();
            case TimeValue:
                long duration = in.readLong();
                TimeUnit unit = TimeUnit.valueOf(in.readString());
                return new TimeValue(duration, unit);
            case ByteSizeValue:
                return new ByteSizeValue(in);
            case Version:
                return Version.readVersion(in);
            default:
                // This Should Never Happen (TM)
                throw new IllegalArgumentException("A SettingType has been added to the enum and not handled here.");
        }
    }

    @Override
    public String toString() {
        return "WriteableSettings{type=Setting<" + type + ">, setting=" + setting + "}";
    }
}
