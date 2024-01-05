package org.scoverage.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspired by scala-maven-plugin's VersionNumber.java
 */
public class ScalaVersion {
    private static final Pattern regexp = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?([-\\.].+)?");

    /**
     * The full version number, including the modifier if any.
     */
    public String full;

    /**
     * The binary compatible version number for this Scala version.
     * e.g. for 2.10.0-M1, this would be 2.10.0-M1, for 2.10.0, this would be 2.10, for 3.3.1, this would be 3.
     */
    public String compatible;

    /**
     * The major Scala version number. e.g. for 2.10.0-M1, this would be 2.
     */
    public int major;

    /**
     * The minor Scala version number. e.g. for 2.10.0-M1, this would be 10.
     */
    public int minor;

    /**
     * The bugfix Scala version number. e.g. for 2.10.0-M1, this would be 0.
     */
    public int bugfix;

    /**
     * The modifier for this Scala version. e.g. for 2.10.0-M1, this would be M1.
     */
    public String modifier;

    /**
     * Creates a ScalaVersion from a String.
     *
     * @param s String to parse
     */
    public ScalaVersion(String s) {
        full = s;
        // parse
        Matcher match = regexp.matcher(s);
        if (!match.find()) {
            throw new IllegalArgumentException("Invalid Scala version [" + s + "]. Expected major.minor(.bugfix)(modifier)");
        }
        major = Integer.parseInt(match.group(1));
        minor = Integer.parseInt(match.group(2));
        if ((match.group(3) != null) && (match.group(3).length() > 1)) {
            bugfix = Integer.parseInt(match.group(3).substring(1));
        }
        if ((match.group(4) != null) && (match.group(4).length() > 1)) {
            modifier = match.group(4);
        }
        // compute compatible
        compatible =
                modifier != null ? full : // non-stable versions are not compatible with anything else
                        isScala2() ? major + "." + minor : // Scala 2.X.Y is compatible with any Scala 2.X.Z
                                major + ""; // Scala 3.X is compatible with any Scala 3.Y
    }

    /**
     * Ignores modifier, so can return `true` for any ScalaVersion with matching major, minor, bugfix.
     *
     * @param other ScalaVersion to compare to
     * @return true if this version is of the same or newer Scala version as the other version
     */
    public boolean isAtLeast(ScalaVersion other) {
        return major >= other.major &&
                (major != other.major || minor >= other.minor) &&
                (major != other.major || minor != other.minor || bugfix >= other.bugfix);

    }

    /**
     * Ignores modifier, so can return `true` for any ScalaVersion with matching major, minor, bugfix.
     *
     * @param scalaVersion to compare to
     * @return true if this version is of the same or newer Scala version as the other version
     */
    public boolean isAtLeast(String scalaVersion) {
        return isAtLeast(new ScalaVersion(scalaVersion));
    }


    /**
     * @return true if this is a Scala 2 version
     */
    public boolean isScala2() {
        return major == 2;
    }

}