package org.lts.demotivator.render;

public record DemotivatorStyle(
        int imageWidth,
        double marginPercent,
        int frameThickness,
        int frameGap,
        String titleFont,
        String subtitleFont,
        double titleSizePercent,
        double subtitleSizePercent,
        boolean uppercaseTitle,
        int backgroundRgb,
        int frameRgb,
        int titleRgb,
        int subtitleRgb) {

    public static DemotivatorStyle classic() {
        return new DemotivatorStyle(1000, 10.0, 3, 4, "Times New Roman", "Arial",
                8.0, 3.6, false, 0x000000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF);
    }
}
