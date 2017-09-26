package er.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import er.api.entities.Eiffel.EiffelEvent;
import er.api.entities.query.Query;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static er.DummyErApplication.eiffelEventRepositories;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@RestController
public class ApiController {

    @RequestMapping(value = "/{filterName}", produces = "application/json; charset=UTF-8")
    public EiffelEvent[] getEiffelEventRepository(@RequestBody(required = false) Query query, @PathVariable String filterName) {
        return getEiffelEventRepositorySpecific(query, filterName, null);
    }

    @RequestMapping(value = "/{filterName}/{metaId}", produces = "application/json; charset=UTF-8")
    public EiffelEvent[] getEiffelEventRepositorySpecific(@RequestBody(required = false) Query query, @PathVariable String filterName, @PathVariable String metaId) {
        if (query == null) {
            System.out.println("No query received, generating standard one.");
            query = new Query();
        } else {
            System.out.println("Query received, max size given: " + query.getSize());
        }

        if (Objects.equals(filterName, "reference-data-set")) {
            if (!eiffelEventRepositories.containsKey(filterName)) {
                try {
                    eiffelEventRepositories.put(filterName, fetchEiffelEventRepository("events.json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!eiffelEventRepositories.containsKey(filterName)) {
            return null;
        }

        if (metaId == null) {
            if (query.getSize() >= eiffelEventRepositories.get(filterName).length) {
                return eiffelEventRepositories.get(filterName);
            }
            return Arrays.copyOfRange(eiffelEventRepositories.get(filterName), 0, query.getSize());
        }

        ArrayList<EiffelEvent> eiffelEvents = new ArrayList<>();
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

        return eiffelEvents.toArray(new EiffelEvent[eiffelEvents.size()]);
    }

    @RequestMapping(value = "/getEiffelEventRepositoriesNames", method = GET, produces = "application/json; charset=UTF-8")
    public Set<String> availableEventRepositories() {
        return eiffelEventRepositories.keySet();
    }

    private EiffelEvent[] fetchEiffelEventRepository(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Resource resource = new ClassPathResource("static/" + file);
        InputStream jsonFileStream = resource.getInputStream();

        EiffelEvent[] eiffelEvents;

        eiffelEvents = mapper.readValue(jsonFileStream, EiffelEvent[].class);

//        System.out.println(String.valueOf(eiffelEvents.length));
        System.out.println("Imported " + eiffelEvents.length + " events from " + file);

        return eiffelEvents;
    }
}
