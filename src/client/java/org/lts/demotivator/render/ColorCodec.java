package org.lts.demotivator.render;

import java.util.Locale;
import java.util.OptionalInt;

public final class ColorCodec {
    private ColorCodec() {
    }

    public static OptionalInt parse(String text) {
        if (text == null) {
            return OptionalInt.empty();
        }
        String value = text.strip();
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.regionMatches(true, 0, "0x", 0, 2)) {
            value = value.substring(2);
        }
        if (value.length() == 3 && value.chars().allMatch(ColorCodec::isHex)) {
            StringBuilder expanded = new StringBuilder(6);
            for (char c : value.toCharArray()) {
                expanded.append(c).append(c);
            }
            value = expanded.toString();
        }
        if (value.length() != 6 || !value.chars().allMatch(ColorCodec::isHex)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(value, 16));
    }

    public static String format(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    public static String normalize(String text, String fallback) {
        OptionalInt parsed = parse(text);
        return parsed.isPresent() ? format(parsed.getAsInt()) : fallback;
    }

    private static boolean isHex(int c) {
        return Character.digit(c, 16) >= 0;
    }
}
