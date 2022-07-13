package net.apltd.copperwiremod.util;

/**
 * Enumeration used to select the target for which the Direction calculation is being done.
 */
public enum RelevantDirMode {
    SOURCE,     //Get the relevant direction on the source
    TARGET,     //Get the relevant direction on the target
    IGNORE,     //Get the direction to ignore power from
    POWER       //Get the direction that power came from
}
