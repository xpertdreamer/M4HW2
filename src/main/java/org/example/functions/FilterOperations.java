package org.example.functions;

import org.example.commands.AppBotCommand;

public class FilterOperations {

    @AppBotCommand(name = "grayScale", description = "greyScale filter", showInKeyboard = true)
    public static float[] grayScale(float[] rgb) {
        final float mean = (rgb[0] + rgb[1] + rgb[2]) / 3;
        rgb[0] = mean;
        rgb[1] = mean;
        rgb[2] = mean;
        return rgb;
    }

    @AppBotCommand(name = "onlyRed", description = "onlyRed filter", showInKeyboard = true)
    public static float[] onlyRed(float[] rgb) {
        rgb[1] = 0;
        rgb[2] = 0;
        return rgb;
    }

    @AppBotCommand(name = "onlyGreen", description = "onlyGreen filter", showInKeyboard = true)
    public static float[] onlyGreen(float[] rgb) {
        rgb[0] = 0;
        rgb[2] = 0;
        return rgb;
    }

    @AppBotCommand(name = "onlyBlue", description = "onlyBlue filter", showInKeyboard = true)
    public static float[] onlyBlue(float[] rgb) {
        rgb[0] = 0;
        rgb[1] = 0;
        return rgb;
    }
}
