package dev.uprweb;

import com.uprfvx.random.Settings;
import com.uprfvx.romio.MiscTweak;
import com.uprfvx.romio.gamedata.ExpCurve;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettingsBridge {
    private SettingsBridge() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }

        switch (args[0]) {
            case "encode":
                if (args.length != 2) {
                    usage();
                    System.exit(1);
                }
                System.out.println(encode(Files.readString(Path.of(args[1]), StandardCharsets.UTF_8)));
                break;
            case "defaults":
                System.out.println(encode("{}"));
                break;
            case "schema":
                System.out.println(schema());
                break;
            default:
                usage();
                System.exit(1);
        }
    }

    private static String encode(String json) throws Exception {
        Settings settings = new Settings();
        applySafeDefaults(settings);
        Map<String, Object> values = Json.parseObject(json);
        int miscTweaks = 0;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            MiscTweak miscTweak = getMiscTweak(entry.getKey());
            if (miscTweak != null) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    miscTweaks |= miscTweak.getValue();
                }
                continue;
            }
            applySetting(settings, entry.getKey(), entry.getValue());
        }

        settings.setCurrentMiscTweaks(miscTweaks);
        return settings.toString();
    }

    private static void applySafeDefaults(Settings settings) {
        settings.setRomName("");
        settings.setAllowWonderGuard(true);
        settings.setTrainersBlockLegendaries(true);
        settings.setTrainersBlockEarlyWonderGuard(true);
        settings.setBlockWildLegendaries(true);
        settings.setCurrentRestrictions(null);
        settings.setSelectedEXPCurve(ExpCurve.MEDIUM_FAST);
        settings.setMakeEvolutionsEasierLvl(Settings.MAKE_EVOLUTIONS_EASIER_DEFAULT_LVL);
    }

    private static void applySetting(Settings settings, String key, Object value) throws Exception {
        String methodName = key.startsWith("set") ? key : "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        List<Method> candidates = new ArrayList<>();

        for (Method method : Settings.class.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1 && Modifier.isPublic(method.getModifiers())) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (parameterType != boolean[].class && canConvert(parameterType, value)) {
                    candidates.add(method);
                }
            }
        }

        candidates.sort(Comparator.comparingInt(method -> conversionRank(method.getParameterTypes()[0])));

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Unsupported setting: " + key);
        }

        Method method = candidates.get(0);
        method.invoke(settings, convert(method.getParameterTypes()[0], value));
    }

    private static boolean canConvert(Class<?> type, Object value) {
        return type == boolean.class && value instanceof Boolean
                || type == int.class && value instanceof Number
                || type == String.class && value instanceof String
                || type.isEnum() && value instanceof String;
    }

    private static int conversionRank(Class<?> type) {
        if (type.isEnum()) {
            return 0;
        }
        if (type == boolean.class || type == int.class || type == String.class) {
            return 1;
        }
        return 2;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Class<?> type, Object value) {
        if (type == boolean.class) {
            return value;
        }
        if (type == int.class) {
            return ((Number) value).intValue();
        }
        if (type == String.class) {
            return value;
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type, ((String) value).toUpperCase());
        }
        throw new IllegalArgumentException("Unsupported value type: " + type.getName());
    }

    private static String schema() {
        return patchedSchema();
    }

    private static String patchedSchema() {
        try {
            Class<?> metadata = Class.forName("com.uprfvx.random.web.SettingsMetadata");
            Method schemaJson = metadata.getMethod("schemaJson");
            return (String) schemaJson.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            throw new IllegalStateException("Patched FVX SettingsMetadata is not available. Build FVX with patches/fvx.");
        }
    }

    private static MiscTweak getMiscTweak(String name) {
        for (MiscTweak tweak : MiscTweak.allTweaks) {
            if (miscTweakName(tweak).equals(name)) {
                return tweak;
            }
        }
        return null;
    }

    private static String miscTweakName(MiscTweak tweak) {
        return "miscTweak_" + tweak.getID();
    }

    private static void usage() {
        System.err.println("Usage: SettingsBridge {defaults|schema|encode <json-file>}");
    }

    private static final class Json {
        private final String input;
        private int index;

        private Json(String input) {
            this.input = input;
        }

        static Map<String, Object> parseObject(String input) {
            Json parser = new Json(input);
            Object value = parser.parseValue();
            parser.skipWhitespace();
            if (parser.index != parser.input.length()) {
                throw new IllegalArgumentException("Unexpected trailing JSON content.");
            }
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("Expected a JSON object.");
            }
            return castObject(value);
        }

        private Object parseValue() {
            skipWhitespace();
            if (peek('{')) {
                return parseMap();
            }
            if (peek('"')) {
                return parseString();
            }
            if (match("true")) {
                return Boolean.TRUE;
            }
            if (match("false")) {
                return Boolean.FALSE;
            }
            if (match("null")) {
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseMap() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index += 1;
                return map;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index += 1;
                    return map;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < input.length()) {
                char current = input.charAt(index++);
                if (current == '"') {
                    return value.toString();
                }
                if (current == '\\') {
                    value.append(parseEscape());
                } else {
                    value.append(current);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private char parseEscape() {
            if (index >= input.length()) {
                throw new IllegalArgumentException("Unterminated JSON escape.");
            }
            char escaped = input.charAt(index++);
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    return escaped;
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                case 'u':
                    return parseUnicodeEscape();
                default:
                    throw new IllegalArgumentException("Invalid JSON escape.");
            }
        }

        private char parseUnicodeEscape() {
            if (index + 4 > input.length()) {
                throw new IllegalArgumentException("Invalid JSON unicode escape.");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index += 1;
            }
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index += 1;
            }
            if (start == index || (input.charAt(start) == '-' && start + 1 == index)) {
                throw new IllegalArgumentException("Expected JSON value.");
            }
            return Integer.parseInt(input.substring(start, index));
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index += 1;
            }
        }

        private boolean match(String value) {
            if (input.startsWith(value, index)) {
                index += value.length();
                return true;
            }
            return false;
        }

        private boolean peek(char expected) {
            return index < input.length() && input.charAt(index) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (!peek(expected)) {
                throw new IllegalArgumentException("Expected '" + expected + "'.");
            }
            index += 1;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> castObject(Object value) {
            return (Map<String, Object>) value;
        }
    }

}
