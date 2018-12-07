package hr.fer.ztel.rassus.dz2.util;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

public class Utility {

    /** Keyword used between two nodes to confirm a receipt of measurement. */
    public static final String RECEIVE_CONFIRMATION = "RECEIVE_CONFIRMATION";
    /** Sleep time for retry logic, in milliseconds. */
    private static final long RETRY_LOGIC_SLEEP_MILLIS = 1000;

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

    /**
     * Compares two ordered lists by comparing elements one by one. The first element
     * that is found different in these two lists is compared and its result is returned.
     * If these two lists are identical (same order and same number of elements), 0 is returned.
     * If one list has less elements than the other, but is a subset of the beginning of the
     * other list, the shorter list is considered 'lower'.
     *
     * @param list1 an ordered list
     * @param list2 an ordered list
     * @param <T> type parameter
     * @return result of comparison
     */
    public static <T extends Comparable<T>> int compareLists(List<T> list1, List<T> list2) {
        int n = Math.min(list1.size(), list2.size());

        for (int i = 0; i < n; i++) {
            int difference = list1.get(i).compareTo(list2.get(i));
            if (difference != 0) {
                return difference;
            }
        }

        if (list1.size() == list2.size()) {
            return 0;  // Elements are equal
        } else if (list1.size() < list2.size()) {
            return -1; // list1 is shorter
        } else {
            return 1;  // list1 is longer
        }
    }

    public static void retry(int times, Callable<Boolean> callable) {
        for (int i = 0; i < times; i++) {
            try {
                boolean success = callable.call();
                if (success) break;
                else sleep(RETRY_LOGIC_SLEEP_MILLIS);
            } catch (IOException e) {
                sleep(RETRY_LOGIC_SLEEP_MILLIS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

}
