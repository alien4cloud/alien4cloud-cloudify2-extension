package alien4cloud.paas.cloudify2.events;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AlienEventDeserializer extends StdDeserializer<AlienEvent> {
    /**
     *
     */
    private static final long serialVersionUID = -3848253635575118061L;
    private static final String TYPE_FIELD = "type";

    private Map<String, Class<? extends AlienEvent>> registry = new HashMap<String, Class<? extends AlienEvent>>();

    public AlienEventDeserializer() {
        super(AlienEvent.class);
        registry.put(EventType.INSTANCE_STATE, AlienEvent.class);
        registry.put(EventType.BLOCKSTORAGE, BlockStorageEvent.class);
    }

    public void registerEvent(String type, Class<? extends AlienEvent> eventClass) {
        registry.put(type, eventClass);
    }

    @Override
    public AlienEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        ObjectNode root = mapper.readTree(jp);
        Class<? extends AlienEvent> eventClass = null;
        String type = EventType.INSTANCE_STATE;
        if (root.hasNonNull(TYPE_FIELD)) {
            type = root.findValue(TYPE_FIELD).asText();
        }
        eventClass = registry.get(type);
        if (eventClass == null) {
            return null;
        }
        return mapper.readValue(jp, eventClass);
    }
}
