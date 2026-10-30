package dev.etran.model

/**
 * A position delta plus rotation delta relative to a base's bed, expressed in the bed's own frame:
 * `right` (to the bed's right), `up`, `fwd` (the direction the bed faces, foot to head).
 */
class Offset(val right: Double, val up: Double, val fwd: Double, val dyaw: Double)
