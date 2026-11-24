package com.smartvillage.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIDASH = Pattern.compile("-+");

    private SlugUtils() {}

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String nowhitespace = WHITESPACE.matcher(input.trim().replace('&', ' ')).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = MULTIDASH.matcher(slug).replaceAll("-");
        slug = slug.replaceAll("^-|-$", "").toLowerCase(Locale.ENGLISH);
        return slug.length() > 80 ? slug.substring(0, 80) : slug;
    }
}
