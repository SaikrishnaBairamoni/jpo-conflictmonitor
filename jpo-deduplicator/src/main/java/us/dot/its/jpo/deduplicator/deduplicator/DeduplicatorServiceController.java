package us.dot.its.jpo.deduplicator.deduplicator;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

import lombok.Getter;
import us.dot.its.jpo.conflictmonitor.monitor.MonitorServiceController;
import us.dot.its.jpo.conflictmonitor.monitor.algorithms.StreamsTopology;
import us.dot.its.jpo.deduplicator.DeduplicatorProperties;
import us.dot.its.jpo.deduplicator.deduplicator.topologies.MapDeduplicatorTopology;
import us.dot.its.jpo.deduplicator.deduplicator.topologies.ProcessedMapDeduplicatorTopology;
import us.dot.its.jpo.deduplicator.deduplicator.topologies.TimDeduplicatorTopology;

@Controller
@DependsOn("createKafkaTopics")
@Profile("!test && !testConfig")
public class DeduplicatorServiceController {

    private static final Logger logger = LoggerFactory.getLogger(MonitorServiceController.class);

    // Temporary for KafkaStreams that don't implement the Algorithm interface
    @Getter
    final ConcurrentHashMap<String, KafkaStreams> streamsMap = new ConcurrentHashMap<String, KafkaStreams>();

    @Getter
    final ConcurrentHashMap<String, StreamsTopology> algoMap = new ConcurrentHashMap<String, StreamsTopology>();

   
    
    @Autowired
    public DeduplicatorServiceController(final DeduplicatorProperties props, 
            final KafkaTemplate<String, String> kafkaTemplate) {
       

        try {

            ProcessedMapDeduplicatorTopology processedMapDeduplicatorTopology = new ProcessedMapDeduplicatorTopology(
                "topic.ProcessedMap",
                "topic.DeduplicatedProcessedMap",
                props.createStreamProperties("ProcessedMapdeduplicator")
            );
            processedMapDeduplicatorTopology.start();

            MapDeduplicatorTopology mapDeduplicatorTopology = new MapDeduplicatorTopology(
                "topic.OdeMapJson",
                "topic.DeduplicatedOdeMapJson",
                props.createStreamProperties("Mapdeduplicator")
            );
            mapDeduplicatorTopology.start();

            TimDeduplicatorTopology timDeduplicatorTopology = new TimDeduplicatorTopology(
                "topic.OdeTimJson",
                "topic.DeduplicatedOdeTimJson",
                props.createStreamProperties("Timdeduplicator")
            );
            timDeduplicatorTopology.start();

        } catch (Exception e) {
            logger.error("Encountered issue with creating topologies", e);
        }
    }
}