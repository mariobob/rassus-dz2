package hr.fer.ztel.rassus.dz2.timestamp;

import lombok.Value;

@Value
public class ScalarTimestamp implements Comparable<ScalarTimestamp> {

    private final long value;

    @Override
    public int compareTo(ScalarTimestamp other) {
        return Long.compare(value, other.value);
    }
}
