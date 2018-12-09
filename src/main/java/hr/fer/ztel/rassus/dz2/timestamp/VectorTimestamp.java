package hr.fer.ztel.rassus.dz2.timestamp;

import hr.fer.ztel.rassus.dz2.util.Utility;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class VectorTimestamp implements Comparable<VectorTimestamp> {

    private final List<Integer> values = new LinkedList<>();

    public VectorTimestamp(int... values) {
        for (int value : values) {
            this.values.add(value);
        }
    }

    /**
     * Combines the two vector timestamps into a new vector timestamp that has the
     * greatest values for each index.
     *
     * @param vector1 a vector timestamp
     * @param vector2 a vector timestamp
     * @return a new vector timestamp
     * @throws IllegalArgumentException if vectors are not of same cardinality
     */
    public static VectorTimestamp combine(VectorTimestamp vector1, VectorTimestamp vector2) {
        if (vector1.values.size() != vector2.values.size()) {
            throw new IllegalArgumentException("Both vectors must be of same cardinality!");
        }

        int[] newValues = new int[vector1.values.size()];
        for (int i = 0, n = vector1.values.size(); i < n; i++) {
            newValues[i] = Math.max(vector1.values.get(i), vector2.values.get(i));
        }

        return new VectorTimestamp(newValues);
    }

    /**
     * Combines the two vector timestamps into a new vector timestamp that has the
     * greatest values for each index. Also sets the value of the specified index
     * explicitly to the given value.
     *
     * @param vector1 a vector timestamp
     * @param vector2 a vector timestamp
     * @param index index of the value to be explicitly set
     * @param indexValue value to be set on the given index
     * @return a new vector timestamp
     * @throws IllegalArgumentException if vectors are not of same cardinality
     */
    public static VectorTimestamp combine(VectorTimestamp vector1, VectorTimestamp vector2, int index, int indexValue) {
        VectorTimestamp newVector = combine(vector1, vector2);
        newVector.values.set(index, indexValue);
        return newVector;
    }

    @Override
    public int compareTo(VectorTimestamp other) {
        if (values.size() != other.values.size()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid timestamp cardinality; expected %d, got %d",
                    values.size(), other.values.size()));
        }

        return Utility.compareLists(values, other.values);
    }
}