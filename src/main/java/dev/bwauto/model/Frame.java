package dev.etran.model;

/**
 * A local coordinate frame anchored on a bed. Using the bed's facing as the frame orientation means a
 * base rotated by any angle (90/180/270 for symmetric maps, or arbitrary) maps onto another base by
 * simply re-anchoring the same local offsets - no per-angle lookup tables.
 *
 * <p>Minecraft yaw: 0 = +Z (south), 90 = -X (west), 180 = -Z, 270 = +X.
 * Forward = (-sin, cos); right = (-cos, -sin).
 *
 * <p>{@code mirror} reflects the base across its own forward axis (the "right" component and the
 * relative yaw are negated). This is the Skript's axis-specific mirroring, expressed in the base frame.
 */
public final class Frame {
    public final double ox;
    public final double oy;
    public final double oz;
    public final double yaw;
    public final boolean mirror;
    private final double sin;
    private final double cos;

    public Frame(double ox, double oy, double oz, double yaw, boolean mirror) {
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.yaw = yaw;
        this.mirror = mirror;
        double r = Math.toRadians(yaw);
        this.sin = Math.sin(r);
        this.cos = Math.cos(r);
    }

    /** World position + yaw -> local offset (the mirror flag is ignored when capturing). */
    public Offset toLocal(double x, double y, double z, double elementYaw) {
        double dx = x - ox;
        double dz = z - oz;
        double fwd = dx * -sin + dz * cos;
        double right = dx * -cos + dz * -sin;
        return new Offset(right, y - oy, fwd, wrap180(elementYaw - yaw));
    }

    /** Local offset -> {x, y, z, yaw} in the world, honouring the mirror flag. */
    public double[] toWorld(Offset o) {
        double r = mirror ? -o.right() : o.right();
        double x = ox + r * -cos + o.fwd() * -sin;
        double z = oz + r * -sin + o.fwd() * cos;
        double y = oy + o.up();
        double outYaw = mirror ? yaw - o.dyaw() : yaw + o.dyaw();
        return new double[]{x, y, z, norm360(outYaw)};
    }

    public static double norm360(double a) {
        double r = a % 360.0;
        return r < 0 ? r + 360.0 : r;
    }

    public static double wrap180(double a) {
        double r = norm360(a);
        return r > 180.0 ? r - 360.0 : r;
    }
}
