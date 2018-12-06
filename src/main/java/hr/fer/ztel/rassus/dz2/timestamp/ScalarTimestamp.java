package hr.fer.ztel.rassus.dz2.timestamp;

import lombok.Value;

@Value
public class ScalarTimestamp implements Comparable<ScalarTimestamp> {

    private final int value;

    @Override
    public int compareTo(ScalarTimestamp other) {
        return Integer.compare(value, other.value);
    }
}
