package er;

import er.api.entities.Eiffel.EiffelEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class DummyErApplication {
    public static ConcurrentHashMap<String, EiffelEvent[]> eiffelEventRepositories = new ConcurrentHashMap<>();

    public static long serverStart = Instant.now().toEpochMilli();
    public static long startTicks = 20;
    public static final long msBetweenTick = 2000;

    public static void main(String[] args) {
        SpringApplication.run(DummyErApplication.class, args);
    }
}
