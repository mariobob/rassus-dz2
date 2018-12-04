package hr.fer.ztel.rassus.dz2.util;

public class Utility {

    /** Disable instantiation. */
    private Utility() {}

    /**
     * Returns the average values between these nullable integers.
     * If only one integers is present, it is returned, otherwise
     * if both integers are <tt>null</tt>, <tt>null</tt> is returned.
     *
     * @param n1 nullable integer
     * @param n2 nullable integer
     * @return the average value of two integers
     */
    public static Integer averageNullableInt(Integer n1, Integer n2) {
        if (n1 != null && n2 != null) {
            return (n1 + n2) / 2;
        } else if (n1 != null) {
            return n1;
        } else if (n2 != null) {
            return n2;
        } else {
            return null;
        }
    }

}
