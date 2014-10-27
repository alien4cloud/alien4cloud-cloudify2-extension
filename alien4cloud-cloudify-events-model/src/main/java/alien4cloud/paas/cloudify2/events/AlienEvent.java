package alien4cloud.paas.cloudify2.events;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({ @Type(value = AlienEvent.class, name = EventType.INSTANCE_STATE_STR), @Type(value = BlockStorageEvent.class, name = EventType.BLOCKSTORAGE_STR) })
public class AlienEvent extends AlienEventDescription {

    private String event;

    public AlienEvent() {
    }

    public AlienEvent(String applicationName, String serviceName, String event) {
        super(applicationName, serviceName);
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
