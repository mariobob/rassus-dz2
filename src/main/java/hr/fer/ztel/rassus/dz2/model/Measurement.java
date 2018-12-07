package hr.fer.ztel.rassus.dz2.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class Measurement {

    private final Integer co;

    public static Measurement parseFromCSV(String s) {
        try {
            // Split tokens and extract required parameters
            String[] tokens = s.split(",", -1);

            // Create builder
            MeasurementBuilder builder = new MeasurementBuilder();

            // Set optional parameters
            if (!tokens[3].isEmpty()) {
                builder.co(Integer.parseInt(tokens[3]));
            }

            return builder.build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not parse string as measurement: " + s, e);
        }
    }

}
