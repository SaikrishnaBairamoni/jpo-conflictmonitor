package us.dot.its.jpo.conflictmonitor.monitor.serialization;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import us.dot.its.jpo.conflictmonitor.monitor.models.processors.BsmAggregator;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.deserialization.BsmAggregatorDeserializer;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.deserialization.OdeBsmDataJsonDeserializer;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.OdeSpatDataJsonDeserializer;
import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;
import us.dot.its.jpo.ode.model.OdeBsmData;

public class JsonSerdes {
   
    public static Serde<OdeBsmData> OdeBsm() {
        return Serdes.serdeFrom(
            new JsonSerializer<OdeBsmData>(), 
            new OdeBsmDataJsonDeserializer());
    }

    public static Serde<BsmAggregator> BsmDataAggregator() {
        return Serdes.serdeFrom(
            new JsonSerializer<BsmAggregator>(), 
            new BsmAggregatorDeserializer());
    }
}
