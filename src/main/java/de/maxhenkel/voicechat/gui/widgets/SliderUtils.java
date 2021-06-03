package de.maxhenkel.voicechat.gui.widgets;

import net.minecraft.util.Mth;

public class SliderUtils {

    public static double toPct(double val, double minValue, double maxValue, float steps) {
        return Mth.clamp((clamp(val, minValue, maxValue, steps) - minValue) / (maxValue - minValue), 0.0D, 1.0D);
    }

    public static double toValue(double d, double minValue, double maxValue, float steps) {
        return clamp(Mth.lerp(Mth.clamp(d, 0.0D, 1.0D), minValue, maxValue), minValue, maxValue, steps);
    }

    private static double clamp(double d, double minValue, double maxValue, float steps) {
        if (steps > 0.0F) {
            d = (steps * (float) Math.round(d / (double) steps));
        }

        return Mth.clamp(d, minValue, maxValue);
    }
}
