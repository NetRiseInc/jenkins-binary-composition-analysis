package io.jenkins.plugins.netrise.asset.uploader.json;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility to serialize / parse JSON objects
 * */
public class JsonMapper {

    private static final Set<Class<?>> PRIMITIVE_INTEGER_TYPES = Set.of(byte.class, short.class, int.class, long.class);
    private static final Set<Class<?>> PRIMITIVE_REAL_TYPES = Set.of(float.class, double.class);

    private static  final Pattern pattern = Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"([^\"]+)\"|(true)|(false)|([\\d\\.]+)|\\[[^\\[]+])", Pattern.MULTILINE);

    static Map<String, String> parseJson(String json) {
        Matcher matcher = pattern.matcher(json);
        Map<String, String> result = new HashMap<>();

        while (matcher.find()) {
            if (matcher.group() != null) {
                String key = matcher.group(1);
                String val = matcher.group(3) != null ? matcher.group(3) : matcher.group(2);
                result.put(key, val);
            }
        }

        return result;
    }

    /**
     * Parse JSON string into the Java object which class is provided as parameter
     * This function is not completed because it parses only the last object in nesting structure.
     * For example {
     *     "level1": {
     *         "a": "aaa"
     *         "level2": {
     *             "b": "bbb",
     *             "c": "ccc"
     *         }
     *     }
     * }
     * will be parsed into Object[b="bbb", c="ccc"]
     *
     * @param json JSON string
     * @param clz Class of the object containing parsed data
     *
     * @return Parsed object
     * */
    public static <T> T parseJson(String json, Class<T> clz) {
        if (json == null) {
            throw new IllegalArgumentException("'json' cannot be null");
        }
        if (clz == null) {
            throw new IllegalArgumentException("'clz' cannot be null");
        }

        Map<String, String> result = parseJson(json);

        T res = null;
        try {
            List<Object> args = new ArrayList<>();
            List<Class<?>> types = new ArrayList<>();
            for (Field field : clz.getDeclaredFields()) {
                String key = getName(field);
                Class<?> type = field.getType();

                String value = result.get(key);


                args.add(getValueByType(value, type));
                types.add(type);
            }
            res = clz.getDeclaredConstructor( types.toArray(new Class<?>[]{}) ).newInstance( args.toArray() );
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // do nothing
        }

        return res;
    }

    /**
     * Generate JSON string from the input data
     *
     * @param data Data object that can be any Java object or in particular Map
     *
     * @return JSON string
     * */
    public static <T> String toJson(T data) {
        return mapObject(data);
    }

    static <T> String mapObject(T o) {
        return mapObject(o, false);
    }

    static <T> String mapObject(T o, boolean nested) {
        if (o == null) {
            return null;
        }
        if (o instanceof Map) {
            return ((Map<?, ?>) o).entrySet().stream()
                    .map(e -> String.format("\"%s\":\"%s\"", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        } else if (CharSequence.class.isAssignableFrom(o.getClass()) || Number.class.isAssignableFrom(o.getClass()) || o instanceof Boolean) {
            return nested ? String.format("\"%s\"", o) : String.valueOf(o);
        }
        List<String> sb = new ArrayList<>();
        mapObject(o, o.getClass(), sb);

        return sb.stream().collect(Collectors.joining(",", "{", "}"));
    }

    private static void mapObject(Object o, Class<?> clz, List<String> sb) {
        if (o != null && clz != null && sb != null) {
            for (Field field : clz.getDeclaredFields()) {
                String key = getName(field);
                String value = null;
                if (key == null) {
                    continue;
                }

                try {
                    value = getValue(o, field);
                } catch (IllegalAccessException e) {
                    // do nothing
                }

                if (value != null) {
                    sb.add(String.format("\"%s\":%s", key, value));
                }
            }

            if (clz.getSuperclass() != null) {
                mapObject(o, clz.getSuperclass(), sb);
            }
        }
    }

    private static String getName(Field field) {
        String key = null;
        if (field != null) {
            JsonProperty jp = field.getAnnotation(JsonProperty.class);
            if (jp != null) {
                if (!jp.enabled()) {
                    return null;
                }
                if (jp.value() != null) {
                    key = jp.value();
                }
            } else {
                key = field.getName();
            }
        }

        return key;
    }

    private static boolean isPrimitive(Class<?> clz) {
        return PRIMITIVE_INTEGER_TYPES.contains(clz) || PRIMITIVE_REAL_TYPES.contains(clz) || char.class.equals(clz) || boolean.class.equals(clz);
    }

    private static String getValue(Object o, Field field) throws IllegalAccessException {
        if (field != null && o != null) {
            field.setAccessible(true);
            Object v = field.get(o);

            if (v != null) {
                if (Boolean.class.equals(field.getType()) || Number.class.isAssignableFrom(field.getType()) || isPrimitive(field.getType())) {
                    return "" + v;
                } else if (CharSequence.class.isAssignableFrom(v.getClass())) {
                    return String.format("\"%s\"", field.get(o));
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    return ((Collection<?>) v).stream().map(p -> mapObject(p, true)).collect(Collectors.joining(",", "[", "]"));
                } else if (Collection.class.isAssignableFrom(field.getType()) || field.getType().isArray()) {
                    return Stream.of(v).map(p -> mapObject(p, true)).collect(Collectors.joining(",", "[", "]"));
                } else {
                    return mapObject(v, true);
                }
            }
        }
        return null;
    }

    private static Object getValueByType(String value, Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("'type' should be defined.");
        }

        if (value == null) {
            return null;
        } else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return Boolean.parseBoolean(value);
        } else if (Integer.class.equals(type) || int.class.equals(type)) {
            return Integer.parseInt(value);
        } else if (Long.class.equals(type) || long.class.equals(type)) {
            return Long.parseLong(value);
        } else if (String.class.equals(type)) {
            return value;
        } else {
            throw new UnsupportedOperationException("Class " + type + " is not supported in JSON object.");
        }
    }

}
