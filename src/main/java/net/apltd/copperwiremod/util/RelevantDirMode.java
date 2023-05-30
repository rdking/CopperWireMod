package net.apltd.copperwiremod.util;

/**
 * Enumeration used to select the target for which the Direction calculation is being done.
 */
public enum RelevantDirMode {
    /**
     * Used to determine the horizontal direction from which the power needs to be read.
     */
    POWER,
    /**
     * Used to determine the direction in which the target can be found.
     */
    POSITION
}
