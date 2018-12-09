package hr.fer.ztel.rassus.dz2.stupidudp.network;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class DecoratedEmulatedSystemClock extends EmulatedSystemClock {

    private static final long AVERAGE_DELAY_MILLIS = 1000L;

    private final EmulatedSystemClock clock;
    private long offset;

    @Override
    public long currentTimeMillis() {
        return clock.currentTimeMillis() + offset + AVERAGE_DELAY_MILLIS;
//        return ((clock.currentTimeMillis() + offset + AVERAGE_DELAY_MILLIS) - startTime) / 1000; // convert to seconds
    }
}
