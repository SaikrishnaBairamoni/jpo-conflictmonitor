package us.dot.its.jpo.conflictmonitor.monitor.topologies;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.kafka.streams.kstream.Produced;

import us.dot.its.jpo.conflictmonitor.ConflictMonitorProperties;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.BaseStreamsTopology;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.connection_of_travel.ConnectionOfTravelAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.connection_of_travel.ConnectionOfTravelParameters;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.intersection_event.IntersectionEventStreamsAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.lane_direction_of_travel.LaneDirectionOfTravelAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.lane_direction_of_travel.LaneDirectionOfTravelParameters;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.signal_state_vehicle_crosses.SignalStateVehicleCrossesAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.signal_state_vehicle_crosses.SignalStateVehicleCrossesParameters;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.signal_state_vehicle_stops.SignalStateVehicleStopsAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.signal_state_vehicle_stops.SignalStateVehicleStopsParameters;
import us.dot.its.jpo.conflictmonitor.monitor.models.VehicleEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.Intersection.Intersection;
import us.dot.its.jpo.conflictmonitor.monitor.models.Intersection.VehiclePath;
import us.dot.its.jpo.conflictmonitor.monitor.models.VehicleEventKey;
import us.dot.its.jpo.conflictmonitor.monitor.models.bsm.*;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.ConnectionOfTravelEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.LaneDirectionOfTravelEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.StopLinePassageEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.SignalStateStopEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.spat.SpatAggregator;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;
import us.dot.its.jpo.geojsonconverter.pojos.spat.ProcessedSpat;
import us.dot.its.jpo.ode.model.OdeBsmData;
import us.dot.its.jpo.ode.plugin.j2735.J2735Bsm;
import static us.dot.its.jpo.conflictmonitor.monitor.algorithms.intersection_event.IntersectionEventConstants.*;

@Component(DEFAULT_INTERSECTION_EVENT_ALGORITHM)
public class IntersectionEventTopology
    extends BaseStreamsTopology<Void>
    implements IntersectionEventStreamsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(IntersectionEventTopology.class);



    ConflictMonitorProperties conflictMonitorProps;

    ReadOnlyWindowStore<BsmIntersectionKey, OdeBsmData> bsmWindowStore;
    ReadOnlyWindowStore<RsuIntersectionKey, ProcessedSpat> spatWindowStore;
    ReadOnlyKeyValueStore<RsuIntersectionKey, ProcessedMap<LineString>> mapStore;
    LaneDirectionOfTravelAlgorithm laneDirectionOfTravelAlgorithm;
    LaneDirectionOfTravelParameters laneDirectionOfTravelParams;
    ConnectionOfTravelAlgorithm connectionOfTravelAlgorithm;
    ConnectionOfTravelParameters connectionOfTravelParams;
    SignalStateVehicleCrossesAlgorithm signalStateVehicleCrossesAlgorithm;
    SignalStateVehicleCrossesParameters signalStateVehicleCrossesParameters;
    SignalStateVehicleStopsAlgorithm signalStateVehicleStopsAlgorithm;
    SignalStateVehicleStopsParameters signalStateVehicleStopsParameters;


    @Override
    protected Logger getLogger() {
        return logger;
    }



    @Override
    protected void validate() {
        if (streamsProperties == null) throw new IllegalStateException("Streams properties are not set.");
        if (bsmWindowStore == null) throw new IllegalStateException("bsmWindowStore is not set.");
        if (spatWindowStore == null) throw new IllegalStateException("spatWindowStore is not set.");
        if (mapStore == null) throw new IllegalStateException("mapStore is not set.");
        if (laneDirectionOfTravelAlgorithm == null) throw new IllegalStateException("LaneDirectionOfTravelAlgorithm is not set");
        if (laneDirectionOfTravelParams == null) throw new IllegalStateException("LaneDirectionOfTravelParameters is not set");
        if (connectionOfTravelAlgorithm == null) throw new IllegalStateException("ConnectionOfTravelAlgorithm is not set");
        if (connectionOfTravelParams == null) throw new IllegalStateException("ConnectionOfTravelParameters is not set");
        if (signalStateVehicleCrossesAlgorithm == null) throw new IllegalStateException("SignalStateVehicleCrossesAlgorithm is not set");
        if (signalStateVehicleCrossesParameters == null) throw new IllegalStateException("SignalStateVehicleCrossesParameters is not set");
        if (signalStateVehicleStopsAlgorithm == null) throw new IllegalStateException("SignalStateVehicleStopsAlgorithm is not set");
        if (signalStateVehicleStopsParameters == null) throw new IllegalStateException("SignalStateVehicleStopsParameters is not set");
        if (streams != null && streams.state().isRunningOrRebalancing()) throw new IllegalStateException("Start called while streams is already running.");
    }




    @Override
    public ConflictMonitorProperties getConflictMonitorProperties() {
        return conflictMonitorProps;
    }

    @Override
    public void setConflictMonitorProperties(ConflictMonitorProperties conflictMonitorProps) {
        this.conflictMonitorProps = conflictMonitorProps;
    }




    @Override
    public LaneDirectionOfTravelAlgorithm getLaneDirectionOfTravelAlgorithm() {
        return laneDirectionOfTravelAlgorithm;
    }

    @Override
    public LaneDirectionOfTravelParameters getLaneDirectionOfTravelParams() {
        return laneDirectionOfTravelParams;
    }

    @Override
    public ConnectionOfTravelAlgorithm getConnectionOfTravelAlgorithm() {
        return connectionOfTravelAlgorithm;
    }

    @Override
    public ConnectionOfTravelParameters getConnectionOfTravelParams() {
        return connectionOfTravelParams;
    }

    @Override
    public SignalStateVehicleCrossesAlgorithm getSignalStateVehicleCrossesAlgorithm() {
        return signalStateVehicleCrossesAlgorithm;
    }

    @Override
    public SignalStateVehicleCrossesParameters getSignalStateVehicleCrossesParameters() {
        return signalStateVehicleCrossesParameters;
    }

    @Override
    public SignalStateVehicleStopsAlgorithm getSignalStateVehicleStopsAlgorithm() {
        return signalStateVehicleStopsAlgorithm;
    }

    @Override
    public SignalStateVehicleStopsParameters getSignalStateVehicleStopsParameters() {
        return signalStateVehicleStopsParameters;
    }

    @Override
    public ReadOnlyWindowStore<BsmIntersectionKey, OdeBsmData> getBsmWindowStore() {
        return bsmWindowStore;
    }

    @Override
    public ReadOnlyWindowStore<RsuIntersectionKey, ProcessedSpat> getSpatWindowStore() {
        return spatWindowStore;
    }

    @Override
    public ReadOnlyKeyValueStore<RsuIntersectionKey, ProcessedMap<LineString>> getMapStore() {
        return mapStore;
    }

    @Override
    public void setBsmWindowStore(ReadOnlyWindowStore<BsmIntersectionKey, OdeBsmData> bsmStore) {
        this.bsmWindowStore = bsmStore;
    }

    @Override
    public void setSpatWindowStore(ReadOnlyWindowStore<RsuIntersectionKey, ProcessedSpat> spatStore) {
        this.spatWindowStore = spatStore;
    }

    @Override
    public void setMapStore(ReadOnlyKeyValueStore<RsuIntersectionKey, ProcessedMap<LineString>> mapStore) {
        this.mapStore = mapStore;
    }


    @Override
    public void setLaneDirectionOfTravelAlgorithm(LaneDirectionOfTravelAlgorithm laneAlgorithm) {
        this.laneDirectionOfTravelAlgorithm = laneAlgorithm;
    }

    @Override
    public void setLaneDirectionOfTravelParams(LaneDirectionOfTravelParameters laneParams) {
        this.laneDirectionOfTravelParams = laneParams;
    }

    @Override
    public void setConnectionOfTravelAlgorithm(ConnectionOfTravelAlgorithm connTravelAlgorithm) {
        this.connectionOfTravelAlgorithm = connTravelAlgorithm;
    }

    @Override
    public void setConnectionOfTravelParams(ConnectionOfTravelParameters connTravelParams) {
        this.connectionOfTravelParams = connTravelParams;
    }

    @Override
    public void setSignalStateVehicleCrossesAlgorithm(SignalStateVehicleCrossesAlgorithm crossesAlgorithm) {
        this.signalStateVehicleCrossesAlgorithm = crossesAlgorithm;
    }

    @Override
    public void setSignalStateVehicleCrossesParameters(SignalStateVehicleCrossesParameters crossesParams) {
        this.signalStateVehicleCrossesParameters = crossesParams;
    }

    @Override
    public void setSignalStateVehicleStopsAlgorithm(SignalStateVehicleStopsAlgorithm stopsAlgorithm) {
        this.signalStateVehicleStopsAlgorithm = stopsAlgorithm;
    }

    @Override
    public void setSignalStateVehicleStopsParameters(SignalStateVehicleStopsParameters stopsParams) {
        this.signalStateVehicleStopsParameters = stopsParams;
    }

    




    private static String getBsmID(OdeBsmData value){
        return ((J2735Bsm)value.getPayload().getData()).getCoreData().getId();
    }

    private static BsmAggregator getBsmsByTimeVehicle(ReadOnlyWindowStore<BsmIntersectionKey, OdeBsmData> bsmWindowStore, Instant start, Instant end, String id){
        logger.info("getBsmsByTimeVehicle: Start: {}, End: {}, ID: {}", start, end, id);

        Instant timeFrom = start.minusSeconds(60);
        Instant timeTo = start.plusSeconds(60);

        long startMillis = start.toEpochMilli();
        long endMillis = end.toEpochMilli();

        KeyValueIterator<Windowed<BsmIntersectionKey>, OdeBsmData> bsmRange = bsmWindowStore.fetchAll(timeFrom, timeTo);

        BsmAggregator agg = new BsmAggregator();

        while(bsmRange.hasNext()){
            KeyValue<Windowed<BsmIntersectionKey>, OdeBsmData> next = bsmRange.next();
            long ts = BsmTimestampExtractor.getBsmTimestamp(next.value);
            if(startMillis <= ts && endMillis >= ts && getBsmID(next.value).equals(id)){
                agg.add(next.value);
            }
            
        }

        bsmRange.close();
        agg.sort();

        logger.info("Found {} BSMs", agg.getBsms().size());

        return agg;
    }

    private static SpatAggregator getSpatByTime(ReadOnlyWindowStore<RsuIntersectionKey, ProcessedSpat> spatWindowStore, Instant start,
                                                Instant end, Integer intersection){

        logger.info("getSpatByTime: Start: {}, End: {}, IntersectionId: {}", start, end, intersection);

        KeyValueIterator<Windowed<RsuIntersectionKey>, ProcessedSpat> spatRange = spatWindowStore.fetchAll(start, end);

        List<ProcessedSpat> allSpats = new ArrayList<>();

        SpatAggregator spatAggregator = new SpatAggregator();

        while(spatRange.hasNext()){
            KeyValue<Windowed<RsuIntersectionKey>, ProcessedSpat> next = spatRange.next();

            ProcessedSpat spat = next.value;
            if (intersection != null && Objects.equals(spat.getIntersectionId(), intersection)) {
                spatAggregator.add(spat);
            }

            allSpats.add(spat);
        }
        spatRange.close();
        spatAggregator.sort();

        logger.info("Total SPATs: {}", allSpats.size());
        logger.info("Found {} SPATs", spatAggregator.getSpats().size());

        return spatAggregator;
    }



    private static ProcessedMap<LineString> getMap(ReadOnlyKeyValueStore<RsuIntersectionKey, ProcessedMap<LineString>> mapStore, RsuIntersectionKey key){
        return (ProcessedMap<LineString>) mapStore.get(key);
    }

    @Override
    public Topology buildTopology() {
        
        StreamsBuilder builder = new StreamsBuilder();

        
        KStream<BsmEventIntersectionKey, BsmEvent> bsmEventStream =
            builder.stream(
                conflictMonitorProps.getKafkaTopicCmBsmEvent(), 
                Consumed.with(
                    JsonSerdes.BsmEventIntersectionKey(),
                    JsonSerdes.BsmEvent())
                )
                    // Filter out BSM Events that aren't inside any MAP bounding box
                    .filter(
                            (key, value) -> value != null && value.isInMapBoundingBox()
            );

        bsmEventStream.print(Printed.toSysOut());



 
        // Join Spats, Maps and BSMS
        KStream<VehicleEventKey, VehicleEvent> vehicleEventsStream = bsmEventStream.flatMap(
            (key, value)->{

                
                List<KeyValue<VehicleEventKey, VehicleEvent>> result = new ArrayList<>();

                
                

                if(value.getStartingBsm() == null || value.getEndingBsm() == null){
                    return result;
                }

                String vehicleId = getBsmID(value.getStartingBsm());
                

                Instant firstBsmTime = Instant.ofEpochMilli(BsmTimestampExtractor.getBsmTimestamp(value.getStartingBsm()));
                Instant lastBsmTime = Instant.ofEpochMilli(BsmTimestampExtractor.getBsmTimestamp(value.getEndingBsm()));

                ProcessedMap map = null;
                BsmAggregator bsms = getBsmsByTimeVehicle(bsmWindowStore, firstBsmTime, lastBsmTime, vehicleId);

                SpatAggregator spats = getSpatByTime(spatWindowStore, firstBsmTime, lastBsmTime, key.getIntersectionId());

                // Find the MAP for the BSMs


                if(spats.getSpats().size() > 0){
                    RsuIntersectionKey rsuKey = new RsuIntersectionKey();
                    rsuKey.setRsuId(key.getRsuId());
                    rsuKey.setIntersectionId(key.getIntersectionId());

                    map = getMap(mapStore, rsuKey);

                    

                    if(map != null){

                        logger.info("Found MAP: {}", map);
                        
                        Intersection intersection = Intersection.fromProcessedMap(map);

                        logger.info("Got Intersection object from MAP: {}", intersection);

                        VehicleEvent event = new VehicleEvent(bsms, spats, intersection);

                        VehicleEventKey vehicleEventKey = new VehicleEventKey();
                        vehicleEventKey.setRsuId(key.getRsuId());
                        vehicleEventKey.setIntersectionId(key.getIntersectionId());
                        vehicleEventKey.setVehicleId(vehicleId);

                        result.add(new KeyValue<>(vehicleEventKey, event));
    
                        
                    }else{
                        System.out.println("Map was Null");
                    }

                }


                logger.info("Detected Vehicle Event");
                logger.info("Vehicle ID: {}", vehicleId);
                logger.info("Captured Bsms: {}", bsms.getBsms().size());
                logger.info("Captured Spats: {}", spats.getSpats().size());

                return result;
            }
        );


        // Perform Analytics on Lane direction of Travel Events
        KStream<String, LaneDirectionOfTravelEvent> laneDirectionOfTravelEventStream = vehicleEventsStream.flatMap(
            (key, value)->{
                VehiclePath path = new VehiclePath(value.getBsms(), value.getIntersection());

                List<KeyValue<String, LaneDirectionOfTravelEvent>> result = new ArrayList<KeyValue<String, LaneDirectionOfTravelEvent>>();
                ArrayList<LaneDirectionOfTravelEvent> events = laneDirectionOfTravelAlgorithm.getLaneDirectionOfTravelEvents(laneDirectionOfTravelParams, path);
                
                for(LaneDirectionOfTravelEvent event: events){
                    result.add(new KeyValue<>(event.getKey(), event));
                }
                return result;
            }
        );

        logger.info("LaneDirectionOfTravelEventStream: {}", laneDirectionOfTravelEventStream);
        laneDirectionOfTravelEventStream.to(
            conflictMonitorProps.getKafkaTopicCmLaneDirectionOfTravelEvent(), 
            Produced.with(Serdes.String(),
                    JsonSerdes.LaneDirectionOfTravelEvent()));

        

        // Perform Analytics on Lane direction of Travel Events
        KStream<String, ConnectionOfTravelEvent> connectionTravelEventsStream = vehicleEventsStream.flatMap(
            (key, value)->{
                VehiclePath path = new VehiclePath(value.getBsms(), value.getIntersection());

                List<KeyValue<String, ConnectionOfTravelEvent>> result = new ArrayList<KeyValue<String, ConnectionOfTravelEvent>>();
                ConnectionOfTravelEvent event = connectionOfTravelAlgorithm.getConnectionOfTravelEvent(connectionOfTravelParams, path);
                if(event != null){
                    result.add(new KeyValue<>(event.getKey(), event));
                }
                return result;
                
            }
        );

        connectionTravelEventsStream.to(
            conflictMonitorProps.getKafkaTopicCmConnectionOfTravelEvent(), 
            Produced.with(Serdes.String(),
                    JsonSerdes.ConnectionOfTravelEvent()));


        // Perform Analytics of Signal State Vehicle Crossing Intersection
        KStream<String, StopLinePassageEvent> signalStateVehicleCrossingEventsStream = vehicleEventsStream.flatMap(
            (key, value)->{
                VehiclePath path = new VehiclePath(value.getBsms(), value.getIntersection());

                List<KeyValue<String, StopLinePassageEvent>> result = new ArrayList<>();
                StopLinePassageEvent event = signalStateVehicleCrossesAlgorithm.getSignalStateEvent(signalStateVehicleCrossesParameters, path, value.getSpats());
                if(event != null){
                    result.add(new KeyValue<>(event.getKey(), event));
                }

                System.out.println("Signal State Event Vehicle Crossing: " + result.size());
                return result;
            }
        );

        signalStateVehicleCrossingEventsStream.to(
            conflictMonitorProps.getKafkaTopicCmSignalStateEvent(), 
            Produced.with(Serdes.String(),
                    JsonSerdes.SignalStateEvent()));



        // Perform Analytics of Signal State Vehicle Crossing Intersection
        KStream<String, SignalStateStopEvent> signalStateVehicleStopEventsStream = vehicleEventsStream.flatMap(
            (key, value)->{

                VehiclePath path = new VehiclePath(value.getBsms(), value.getIntersection());

                List<KeyValue<String, SignalStateStopEvent>> result = new ArrayList<KeyValue<String, SignalStateStopEvent>>();
                SignalStateStopEvent event = signalStateVehicleStopsAlgorithm.getSignalStateStopEvent(signalStateVehicleStopsParameters, path, value.getSpats());
                if(event != null){
                    result.add(new KeyValue<>(event.getKey(), event));
                }

                
                return result;
            }
        );

        signalStateVehicleStopEventsStream.to(
            conflictMonitorProps.getKafakTopicCmVehicleStopEvent(), 
            Produced.with(Serdes.String(),
                    JsonSerdes.SignalStateVehicleStopsEvent()));
 
        return builder.build();
    }


    
}
