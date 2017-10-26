package er.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.*;
import er.api.entities.Eiffel.EiffelEvent;
import er.api.entities.query.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static er.DummyErApplication.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@RestController
public class ApiController {

    @Value("${mongodb.host}")
    private String host;
    @Value("${mongodb.port}")
    private int port;

    @Value("${mongodb}")
    private boolean useMongoDB;

    @Value("${database.name}")
    private String dbName;

    @Value("${collection.name}")
    private String collectionName;

    @RequestMapping(value = "/{filterName}", produces = "application/json; charset=UTF-8")
    public EiffelEvent[] getEiffelEventRepository(@RequestBody(required = false) Query query, @PathVariable String filterName) {
        return getEiffelEventRepositorySpecific(query, filterName, null);
    }

    private long getSortTime(EiffelEvent event) {
        return event.getMeta().getTime();
    }

    @RequestMapping(value = "/{filterName}/{metaId}", produces = "application/json; charset=UTF-8")
    public EiffelEvent[] getEiffelEventRepositorySpecific(@RequestBody(required = false) Query query, @PathVariable String filterName, @PathVariable String metaId) {
        if (query == null) {
            System.out.println("No query received, generating standard one.");
            query = new Query();
        } else {
            System.out.println("Query received, max size given: " + query.getSize());
        }

        System.out.println("FilterName: " + filterName);

        ArrayList<EiffelEvent> eiffelEvents = new ArrayList<>();

        System.out.println("Using MongoDB: " + useMongoDB);
        if (useMongoDB) {
            MongoClient mongoClient = new MongoClient(host, port);

            ArrayList<String> allEvents = new ArrayList<>();

            DB db = mongoClient.getDB(dbName);
            DBCollection table = db.getCollection(collectionName);
            DBCursor cursor = table.find();
            if (cursor.count() != 0) {
                int i = 1;
                while (cursor.hasNext()) {
                    DBObject document = cursor.next();
                    String documentStr = document.toString();
                    allEvents.add(documentStr);
                    i++;
                }
            } else {
                System.out.println("No documents found in database: " + dbName + "and collection: " + collectionName);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            for (String stringEvent : allEvents) {
                try {
                    eiffelEvents.add(objectMapper.readValue(stringEvent, EiffelEvent.class));
                } catch (IOException e) {
                    System.out.println(stringEvent);
                    e.printStackTrace();
                }
            }

            mongoClient.close();

            eiffelEvents.sort(Comparator.comparingLong(this::getSortTime));

            System.out.println("Collected " + eiffelEvents.size() + " events.");
        } else {
            Pattern pattern = Pattern.compile("^live\\[(.+)]$");
            Matcher matcher = pattern.matcher(filterName.trim());

            boolean fakeLive = false;

            if (matcher.find()) {
                System.out.println("Found live!");
                filterName = matcher.group(1);
                fakeLive = true;
            }

            if (!eiffelEventRepositories.containsKey(filterName)) {
                try {
                    eiffelEventRepositories.put(filterName, fetchEiffelEventRepository(filterName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (!eiffelEventRepositories.containsKey(filterName)) {
                return null;
            }

            if (fakeLive) {
                int eventsToFetch = (int) (startTicks + ((Instant.now().toEpochMilli() - serverStart) / msBetweenTick));
                System.out.println("Events to fetch: " + eventsToFetch);
                if (eventsToFetch < query.getSize()) {
                    query.setSize(eventsToFetch);
                }
                System.out.println("Faking live, fetching: " + query.getSize());
            }

            if (metaId == null) {
                if (query.getSize() >= eiffelEventRepositories.get(filterName).length) {
                    return eiffelEventRepositories.get(filterName);
                }
                return Arrays.copyOfRange(eiffelEventRepositories.get(filterName), 0, query.getSize());
            }

            int addCount = 0;

            for (EiffelEvent eiffelEvent : eiffelEventRepositories.get(filterName)) {
                if (eiffelEvent.getMeta().getId().equals(metaId)) {
                    eiffelEvents.add(eiffelEvent);
                    addCount++;
                    if (addCount >= query.getSize()) {
                        break;
                    }
                }
            }
        }


        return eiffelEvents.toArray(new EiffelEvent[eiffelEvents.size()]);
    }

    @RequestMapping(value = "/getEiffelEventRepositoriesNames", method = GET, produces = "application/json; charset=UTF-8")
    public Set<String> availableEventRepositories() {
        return eiffelEventRepositories.keySet();
    }

    private EiffelEvent[] fetchEiffelEventRepository(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Resource resource = new ClassPathResource("static/" + file + ".json");
        InputStream jsonFileStream = resource.getInputStream();

        EiffelEvent[] eiffelEvents;

        eiffelEvents = mapper.readValue(jsonFileStream, EiffelEvent[].class);

        Arrays.sort(eiffelEvents, Comparator.comparingLong(o -> o.getMeta().getTime()));

        System.out.println("Imported " + eiffelEvents.length + " events from " + file);

        return eiffelEvents;
    }
}
