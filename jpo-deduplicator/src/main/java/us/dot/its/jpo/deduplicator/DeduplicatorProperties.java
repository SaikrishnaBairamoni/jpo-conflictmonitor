/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.deduplicator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.SystemUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.processor.LogAndSkipOnInvalidTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import us.dot.its.jpo.conflictmonitor.AlwaysContinueProductionExceptionHandler;
import us.dot.its.jpo.ode.eventlog.EventLogger;
import us.dot.its.jpo.ode.util.CommonUtils;

// import us.dot.its.jpo.conflictmonitor.AlwaysContinueProductionExceptionHandler;

@Getter
@Setter
@ConfigurationProperties
public class DeduplicatorProperties implements EnvironmentAware  {

   private static final Logger logger = LoggerFactory.getLogger(DeduplicatorProperties.class);


   private String kafkaTopicProcessedMap;
   private String kafkaTopicDeduplicatedProcessedMap;

   //Processed Map WKT Topic
   private String kafkaTopicProcessedMapWKT;
   private String kafkaTopicDeduplicatedProcessedMapWKT;

   //Ode Map Json Topic
   private String kafkaTopicOdeMapJson;
   private String kafkaTopicDeduplicatedOdeMapJson;

   //Ode Tim Json Topics
   private String kafkaTopicOdeTimJson;
   private String kafkaTopicDeduplicatedOdeTimJson;

   // Confluent Properties
   private boolean confluentCloudEnabled = false;
   private String confluentKey = null;
   private String confluentSecret = null;

   @Autowired
   @Setter(AccessLevel.NONE)
   private Environment env;

   /*
    * General Properties
    */
   private String version;
   // public static final int OUTPUT_SCHEMA_VERSION = 6;
   
   @Setter(AccessLevel.NONE)
   private String kafkaBrokers = null;

   private static final String DEFAULT_KAFKA_PORT = "9092";
   private static final String DEFAULT_CONNECT_PORT = "8083";
   
   @Setter(AccessLevel.NONE)
   private String hostId;

   @Setter(AccessLevel.NONE)
   private String connectURL = null;

   // @Setter(AccessLevel.NONE)
   // private String dockerHostIP = null;

   @Setter(AccessLevel.NONE)
   private String kafkaBrokerIP = null;


   @Setter(AccessLevel.NONE)
   private String dbHostIP = null;


   @Setter(AccessLevel.NONE)
   @Autowired
   BuildProperties buildProperties;

   @PostConstruct
   void initialize() {
      setVersion(buildProperties.getVersion());
      logger.info("groupId: {}", buildProperties.getGroup());
      logger.info("artifactId: {}", buildProperties.getArtifact());
      logger.info("version: {}", version);
      //OdeMsgMetadata.setStaticSchemaVersion(OUTPUT_SCHEMA_VERSION);

      

      String hostname;
      try {
         hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
         // Let's just use a random hostname
         hostname = UUID.randomUUID().toString();
         logger.info("Unknown host error: {}, using random", e);
      }
      hostId = hostname;
      logger.info("Host ID: {}", hostId);
      EventLogger.logger.info("Initializing services on host {}", hostId);

      if(dbHostIP == null){
         String dbHost = CommonUtils.getEnvironmentVariable("DB_HOST_IP");

         if(dbHost == null){
            logger.warn(
                  "DB Host IP not defined, Defaulting to localhost.");
            dbHost = "localhost";
         }
         dbHostIP = dbHost;
      }

      if (kafkaBrokers == null) {

         String kafkaBroker = CommonUtils.getEnvironmentVariable("KAFKA_BROKER_IP");

         logger.info("ode.kafkaBrokers property not defined. Will try KAFKA_BROKER_IP => {}", kafkaBrokers);

         if (kafkaBroker == null) {
            logger.warn(
                  "Neither ode.kafkaBrokers ode property nor KAFKA_BROKER_IP environment variable are defined. Defaulting to localhost.");
            kafkaBroker = "localhost";
         }

         kafkaBrokers = kafkaBroker + ":" + DEFAULT_KAFKA_PORT;
      }

      String kafkaType = CommonUtils.getEnvironmentVariable("KAFKA_TYPE");
      if (kafkaType != null) {
         confluentCloudEnabled = kafkaType.equals("CONFLUENT");
         if (confluentCloudEnabled) {
               
               System.out.println("Enabling Confluent Cloud Integration");

               confluentKey = CommonUtils.getEnvironmentVariable("CONFLUENT_KEY");
               confluentSecret = CommonUtils.getEnvironmentVariable("CONFLUENT_SECRET");
         }
      }

      // Initialize the Kafka Connect URL
      if (connectURL == null) {
         String kafkaBroker = CommonUtils.getEnvironmentVariable("DB_HOST_IP");
         if (kafkaBroker == null) {
            kafkaBroker = "localhost";
         }
         // dockerHostIP = dockerIp;
         kafkaBrokerIP = kafkaBroker;
         connectURL = String.format("http://%s:%s", kafkaBroker, DEFAULT_CONNECT_PORT);
      }

      // List<String> asList = Arrays.asList(this.getKafkaTopicsDisabled());
      // logger.info("Disabled Topics: {}", asList);
      // kafkaTopicsDisabledSet.addAll(asList);
   }

   public Properties createStreamProperties(String name) {
      Properties streamProps = new Properties();
      streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, name);

      streamProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);

      streamProps.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
            LogAndContinueExceptionHandler.class.getName());

      streamProps.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
            LogAndSkipOnInvalidTimestamp.class.getName());

      streamProps.put(StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG,
            AlwaysContinueProductionExceptionHandler.class.getName());

      streamProps.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);

      // streamProps.put(StreamsConfig.producerPrefix("acks"), "all");
      streamProps.put(StreamsConfig.producerPrefix(ProducerConfig.ACKS_CONFIG), "all");

      // Reduce cache buffering per topology to 1MB
      streamProps.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 1 * 1024 * 1024L);

      // Decrease default commit interval. Default for 'at least once' mode of 30000ms
      // is too slow.
      streamProps.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100);

      // All the keys are Strings in this app
      streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

      // Configure the state store location
      if (SystemUtils.IS_OS_LINUX) {
         streamProps.put(StreamsConfig.STATE_DIR_CONFIG, "/var/lib/ode/kafka-streams");
      } else if (SystemUtils.IS_OS_WINDOWS) {
         streamProps.put(StreamsConfig.STATE_DIR_CONFIG, "C:/temp/ode");
      }
      // streamProps.put(StreamsConfig.STATE_DIR_CONFIG, "/var/lib/")\

      // Increase max.block.ms and delivery.timeout.ms for streams
      final int FIVE_MINUTES_MS = 5 * 60 * 1000;
      streamProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, FIVE_MINUTES_MS);
      streamProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, FIVE_MINUTES_MS);

      // Disable batching
      streamProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 0);

      if (confluentCloudEnabled) {
         streamProps.put("ssl.endpoint.identification.algorithm", "https");
         streamProps.put("security.protocol", "SASL_SSL");
         streamProps.put("sasl.mechanism", "PLAIN");

         if (confluentKey != null && confluentSecret != null) {
             String auth = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                 "username=\"" + confluentKey + "\" " +
                 "password=\"" + confluentSecret + "\";";
                 streamProps.put("sasl.jaas.config", auth);
         }
         else {
             logger.error("Environment variables CONFLUENT_KEY and CONFLUENT_SECRET are not set. Set these in the .env file to use Confluent Cloud");
         }
     }


      return streamProps;
   }

   public String getProperty(String key) {
      return env.getProperty(key);
   }

   public String getProperty(String key, String defaultValue) {
      return env.getProperty(key, defaultValue);
   }

   public Object getProperty(String key, int i) {
      return env.getProperty(key, Integer.class, i);
   }

   public Boolean getConfluentCloudStatus() {
		return confluentCloudEnabled;
	}


   @Value("${kafkaTopicProcessedMap}")
   public void setKafkaTopicProcessedMap(String kafkaTopicProcessedMap) {
      this.kafkaTopicProcessedMap = kafkaTopicProcessedMap;
   }

   @Value("${kafkaTopicDeduplicatedProcessedMap}")
   public void setKafkaTopicDeduplicatedProcessedMap(String kafkaTopicDeduplicatedProcessedMap) {
      this.kafkaTopicDeduplicatedProcessedMap = kafkaTopicDeduplicatedProcessedMap;
   }

   @Value("${kafkaTopicProcessedMapWKT}")
   public void setKafkaTopicProcessedMapWKT(String kafkaTopicProcessedMapWKT) {
      this.kafkaTopicProcessedMapWKT = kafkaTopicProcessedMapWKT;
   }

   @Value("${kafkaTopicDeduplicatedProcessedMapWKT}")
   public void setKafkaTopicDeduplicatedProcessedMapWKT(String kafkaTopicDeduplicatedProcessedMapWKT) {
      this.kafkaTopicDeduplicatedProcessedMapWKT = kafkaTopicDeduplicatedProcessedMapWKT;
   }

   @Value("${kafkaTopicOdeMapJson}")
   public void setKafkaTopicOdeMapJson(String kafkaTopicOdeMapJson) {
      this.kafkaTopicOdeMapJson = kafkaTopicOdeMapJson;
   }

   @Value("${kafkaTopicDeduplicatedOdeMapJson}")
   public void setKafkaTopicDeduplicatedOdeMapJson(String kafkaTopicDeduplicatedOdeMapJson) {
      this.kafkaTopicDeduplicatedOdeMapJson = kafkaTopicDeduplicatedOdeMapJson;
   }

   @Value("${kafkaTopicOdeTimJson}")
   public void setKafkaTopicOdeTimJson(String kafkaTopicOdeTimJson) {
      this.kafkaTopicOdeTimJson = kafkaTopicOdeTimJson;
   }

   @Value("${kafkaTopicDeduplicatedOdeTimJson}")
   public void setKafkaTopicDeduplicatedOdeTimJson(String kafkaTopicDeduplicatedOdeTimJson) {
      this.kafkaTopicDeduplicatedOdeTimJson = kafkaTopicDeduplicatedOdeTimJson;
   }

   @Value("${spring.kafka.bootstrap-servers}")
   public void setKafkaBrokers(String kafkaBrokers) {
      this.kafkaBrokers = kafkaBrokers;
   }

   @Override
   public void setEnvironment(Environment environment) {
      env = environment;
   }
}
