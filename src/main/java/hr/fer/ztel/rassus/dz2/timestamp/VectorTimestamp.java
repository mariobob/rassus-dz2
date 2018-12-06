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