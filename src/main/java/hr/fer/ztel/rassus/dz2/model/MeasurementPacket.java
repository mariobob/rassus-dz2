package hr.fer.ztel.rassus.dz2.model;

import hr.fer.ztel.rassus.dz2.timestamp.ScalarTimestamp;
import hr.fer.ztel.rassus.dz2.timestamp.VectorTimestamp;
import lombok.Value;

@Value
public class MeasurementPacket {

    private final Measurement measurement;
    private final ScalarTimestamp scalarTimestamp;
    private final VectorTimestamp vectorTimestamp;

}
