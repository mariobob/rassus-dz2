package hr.fer.ztel.rassus.dz2.loader;

import hr.fer.ztel.rassus.dz2.model.Measurement;

import java.util.List;

public interface MeasurementLoader {

    Measurement getMeasurement(int index);

    List<Measurement> getMeasurements();

}
