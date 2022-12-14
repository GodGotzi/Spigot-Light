package net.minecraft.world.entity.ai.attributes;

import net.minecraft.util.MathHelper;

public class AttributeRanged extends AttributeBase {

    private final double a;
    public double maximum; // Spigot

    public AttributeRanged(String s, double d0, double d1, double d2) {
        super(s, d0);
        this.a = d1;
        this.maximum = d2;
        if (d1 > d2) {
            throw new IllegalArgumentException("Minimum value cannot be bigger than maximum value!");
        } else if (d0 < d1) {
            throw new IllegalArgumentException("Default value cannot be lower than minimum value!");
        } else if (d0 > d2) {
            throw new IllegalArgumentException("Default value cannot be bigger than maximum value!");
        }
    }

    @Override
    public double a(double d0) {
        if (d0 != d0) return getDefault(); // CraftBukkit

        d0 = MathHelper.a(d0, this.a, this.maximum);
        return d0;
    }
}
