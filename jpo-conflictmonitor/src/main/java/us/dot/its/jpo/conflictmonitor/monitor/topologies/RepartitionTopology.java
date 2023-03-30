package us.dot.its.jpo.conflictmonitor.monitor.topologies;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.KafkaStreams.StateListener;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.stereotype.Component;

import us.dot.its.jpo.conflictmonitor.monitor.algorithms.repartition.RepartitionParameters;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.repartition.RepartitionStreamsAlgorithm;
import us.dot.its.jpo.conflictmonitor.monitor.models.bsm.BsmIntersectionKey;
import us.dot.its.jpo.conflictmonitor.monitor.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIdPartitioner;
import us.dot.its.jpo.ode.model.OdeBsmData;
import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static us.dot.its.jpo.conflictmonitor.monitor.algorithms.repartition.RepartitionConstants.*;

import java.util.Properties;


@Component(DEFAULT_REPARTITION_ALGORITHM)
public class RepartitionTopology implements RepartitionStreamsAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(RepartitionTopology.class);

    RepartitionParameters parameters;
    Properties streamsProperties;
    Topology topology;
    KafkaStreams streams;

    @Override
    public void setParameters(RepartitionParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public RepartitionParameters getParameters() {
        return parameters;
    }

    @Override
    public void setStreamsProperties(Properties streamsProperties) {
       this.streamsProperties = streamsProperties;
    }

    @Override
    public Properties getStreamsProperties() {
        return streamsProperties;
    }

    @Override
    public KafkaStreams getStreams() {
        return streams;
    }

    @Override
    public void start() {
        if (parameters == null) {
            throw new IllegalStateException("Start called before setting parameters.");
        }
        if (streamsProperties == null) {
            throw new IllegalStateException("Streams properties are not set.");
        }
        if (streams != null && streams.state().isRunningOrRebalancing()) {
            throw new IllegalStateException("Start called while streams is already running.");
        }
        logger.info("Starting Repartition Topology.");
        Topology topology = buildTopology();
        streams = new KafkaStreams(topology, streamsProperties);
        if (exceptionHandler != null) streams.setUncaughtExceptionHandler(exceptionHandler);
        if (stateListener != null) streams.setStateListener(stateListener);
        streams.start();
        logger.info("Started Repartition Topology");
        System.out.println(parameters.getBsmInputTopicName());
    }

    private Topology buildTopology() {

        StreamsBuilder builder = new StreamsBuilder();
 
        KStream<String, OdeBsmData> bsmRepartitionStream = 
        builder.stream(
            parameters.getBsmInputTopicName(), 
            Consumed.with(
                Serdes.String(),
                JsonSerdes.OdeBsm())
            );


        KStream<BsmIntersectionKey, OdeBsmData> bsmRekeyedStream = bsmRepartitionStream.selectKey((key, value)->{
            String ip = ((OdeBsmMetadata)value.getMetadata()).getOriginIp();
            return new BsmIntersectionKey(ip);
        });

        bsmRekeyedStream.to(
            parameters.getBsmRepartitionOutputTopicName(), 
            Produced.with(
                JsonSerdes.BsmIntersectionKey(),
                JsonSerdes.OdeBsm(),
                new RsuIdPartitioner<BsmIntersectionKey, OdeBsmData>()
            )
        );



    return builder.build();

    }

    @Override
    public void stop() {
        logger.info("Stopping BSMRepartitionTopology.");
        if (streams != null) {
            streams.close();
            streams.cleanUp();
            streams = null;
        }
        logger.info("Stopped BSMRepartitionTopology.");
    }

    
    StateListener stateListener;

    @Override
    public void registerStateListener(StateListener stateListener) {
        this.stateListener = stateListener;
    }

    StreamsUncaughtExceptionHandler exceptionHandler;

    @Override
    public void registerUncaughtExceptionHandler(StreamsUncaughtExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
}
