package dev.verrai.api.binding;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for type converters.
 */
public class ConverterRegistry {
    private static final Map<String, Converter<?, ?>> converters = new HashMap<>();

    static {
        // Integer <-> String
        register(Integer.class, String.class, new Converter<Integer, String>() {
            @Override
            public Integer toModel(String widgetValue) {
                if (widgetValue == null || widgetValue.isEmpty())
                    return null;
                try {
                    return Integer.valueOf(widgetValue);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            @Override
            public String toWidget(Integer modelValue) {
                return modelValue == null ? "" : modelValue.toString();
            }
        });

        // Double <-> String
        register(Double.class, String.class, new Converter<Double, String>() {
            @Override
            public Double toModel(String widgetValue) {
                if (widgetValue == null || widgetValue.isEmpty())
                    return null;
                try {
                    return Double.valueOf(widgetValue);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            @Override
            public String toWidget(Double modelValue) {
                return modelValue == null ? "" : modelValue.toString();
            }
        });

        // Long <-> String
        register(Long.class, String.class, new Converter<Long, String>() {
            @Override
            public Long toModel(String widgetValue) {
                if (widgetValue == null || widgetValue.isEmpty())
                    return null;
                try {
                    return Long.valueOf(widgetValue);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            @Override
            public String toWidget(Long modelValue) {
                return modelValue == null ? "" : modelValue.toString();
            }
        });

        // Boolean <-> String
        register(Boolean.class, String.class, new Converter<Boolean, String>() {
            @Override
            public Boolean toModel(String widgetValue) {
                if (widgetValue == null)
                    return null;
                return Boolean.valueOf(widgetValue);
            }

            @Override
            public String toWidget(Boolean modelValue) {
                return modelValue == null ? "false" : modelValue.toString();
            }
        });
    }

    public static <M, W> void register(Class<M> modelType, Class<W> widgetType, Converter<M, W> converter) {
        converters.put(getKey(modelType, widgetType), converter);
    }

    @SuppressWarnings("unchecked")
    public static <M, W> Converter<M, W> get(Class<M> modelType, Class<W> widgetType) {
        Class<?> effectiveModelType = wrapPrimitive(modelType);
        return (Converter<M, W>) converters.get(getKey(effectiveModelType, widgetType));
    }

    private static String getKey(Class<?> modelType, Class<?> widgetType) {
        return modelType.getName() + "->" + widgetType.getName();
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
        if (!type.isPrimitive())
            return type;
        if (type == int.class)
            return Integer.class;
        if (type == boolean.class)
            return Boolean.class;
        if (type == double.class)
            return Double.class;
        if (type == long.class)
            return Long.class;
        if (type == float.class)
            return Float.class;
        if (type == short.class)
            return Short.class;
        if (type == byte.class)
            return Byte.class;
        if (type == char.class)
            return Character.class;
        return type;
    }
}
