package org.scoverage.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspired by scala-maven-plugin's VersionNumber.java
 */
public class ScalaVersion {
    private static final Pattern regexp = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?([-\\.].+)?");

    public String full;
    public String compatible;
    public int major;
    public int minor;
    public int bugfix;
    public String modifier;

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
     */
    public boolean isAtLeast(ScalaVersion other) {
        return major >= other.major &&
                (major != other.major || minor >= other.minor) &&
                (major != other.major || minor != other.minor || bugfix >= other.bugfix);

    }

    public boolean isAtLeast(String scalaVersion) {
        return isAtLeast(new ScalaVersion(scalaVersion));
    }

    public boolean isScala2() {
        return major == 2;
    }

}