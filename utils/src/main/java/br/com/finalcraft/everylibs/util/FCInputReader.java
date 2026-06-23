package br.com.finalcraft.everylibs.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.UUID;

/**
 * Null-safe parsing helpers that never throw: each {@code parseX} returns a fallback
 * (default {@code null}) instead of propagating a parse exception. Input is trimmed
 * first, and blank/null input always yields the fallback.
 */
public class FCInputReader {

    private FCInputReader() {}

    /** Trims the input and collapses null/blank to {@code null} so every parser shares one guard. */
    private static String clean(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ---- Integer ----------------------------------------------------------------------

    public static Integer parseInt(String input) {
        return parseInt(input, null);
    }

    public static Integer parseInt(String input, Integer def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- Long -------------------------------------------------------------------------

    public static Long parseLong(String input) {
        return parseLong(input, null);
    }

    public static Long parseLong(String input, Long def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- Short ------------------------------------------------------------------------

    public static Short parseShort(String input) {
        return parseShort(input, null);
    }

    public static Short parseShort(String input, Short def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- Byte -------------------------------------------------------------------------

    public static Byte parseByte(String input) {
        return parseByte(input, null);
    }

    public static Byte parseByte(String input, Byte def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- Double -----------------------------------------------------------------------

    public static Double parseDouble(String input) {
        return parseDouble(input, null);
    }

    public static Double parseDouble(String input, Double def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- Float ------------------------------------------------------------------------

    public static Float parseFloat(String input) {
        return parseFloat(input, null);
    }

    public static Float parseFloat(String input, Float def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- BigInteger / BigDecimal ------------------------------------------------------

    public static BigInteger parseBigInteger(String input) {
        return parseBigInteger(input, null);
    }

    public static BigInteger parseBigInteger(String input, BigInteger def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return new BigInteger(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static BigDecimal parseBigDecimal(String input) {
        return parseBigDecimal(input, null);
    }

    public static BigDecimal parseBigDecimal(String input, BigDecimal def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ---- Boolean ----------------------------------------------------------------------

    /**
     * Parses a generous set of truthy/falsy tokens (case-insensitive):
     * {@code true/t/yes/y/on/1} and {@code false/f/no/n/off/0}. Anything else is the fallback.
     */
    public static Boolean parseBoolean(String input) {
        return parseBoolean(input, null);
    }

    public static Boolean parseBoolean(String input, Boolean def) {
        String value = clean(input);
        if (value == null) return def;
        switch (value.toLowerCase(Locale.ROOT)) {
            case "true": case "t": case "yes": case "y": case "on":  case "sim": case "s": case "1":
                return Boolean.TRUE;
            case "false": case "f": case "no": case "n": case "off":  case "nao": case "não": case "0":
                return Boolean.FALSE;
            default:
                return def;
        }
    }

    // ---- UUID -------------------------------------------------------------------------

    public static UUID parseUUID(String input) {
        return parseUUID(input, null);
    }

    public static UUID parseUUID(String input, UUID def) {
        String value = clean(input);
        if (value == null) return def;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    // ---- Enum -------------------------------------------------------------------------

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String input) {
        return parseEnum(type, input, null);
    }

    /** Case-insensitive match against the enum constant names. */
    public static <E extends Enum<E>> E parseEnum(Class<E> type, String input, E def) {
        String value = clean(input);
        if (value == null) return def;
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        return def;
    }

}
