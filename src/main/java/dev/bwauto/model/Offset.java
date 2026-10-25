package dev.etran.model;

/**
 * A position delta plus rotation delta relative to a base's bed, expressed in the bed's own frame:
 * {@code right} (to the bed's right), {@code up}, {@code fwd} (the direction the bed faces, foot to head).
 */
public record Offset(double right, double up, double fwd, double dyaw) {
}
