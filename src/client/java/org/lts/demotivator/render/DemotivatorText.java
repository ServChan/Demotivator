package org.lts.demotivator.render;

public record DemotivatorText(String title, String subtitle) {
    public static final char SEPARATOR = '|';
    public static final int MAX_LENGTH = 256;

    public DemotivatorText {
        title = clean(title);
        subtitle = clean(subtitle);
    }

    public static DemotivatorText parse(String raw, String fallbackSubtitle) {
        String input = raw == null ? "" : raw;
        int split = input.indexOf(SEPARATOR);
        String title = split < 0 ? input : input.substring(0, split);
        String subtitle = split < 0 ? fallbackSubtitle : input.substring(split + 1);
        return new DemotivatorText(title, subtitle);
    }

    public boolean isEmpty() {
        return title.isEmpty() && subtitle.isEmpty();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        String collapsed = value.replace('\t', ' ')
                .replace("\\n", "\n")
                .replaceAll("[\\p{Cntrl}&&[^\n]]", "")
                .replaceAll(" {2,}", " ")
                .replaceAll(" *\n *", "\n")
                .strip();
        return collapsed.length() > MAX_LENGTH ? collapsed.substring(0, MAX_LENGTH).strip() : collapsed;
    }
}
