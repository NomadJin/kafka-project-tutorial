package com.example.kafka;

import com.github.javafaker.Faker;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PizzaProducerCustomPartitioner {

	public static final Logger logger = LoggerFactory.getLogger(PizzaProducerCustomPartitioner.class.getName());

	public static void sendPizzaMessage(KafkaProducer<String, String> kafkaProducer, String topicName, int iterCount, int interIntervalMillis,
			int intervalMillis, int intervalCount, boolean sync) {

		PizzaMessage pizzaMessage = new PizzaMessage();
		int iterSeq = 0;
		long seed = 2022;
		Random random = new Random(seed);
		Faker faker = Faker.instance(random);

		long startTime = System.currentTimeMillis();

		while (iterSeq++ != iterCount) {
			HashMap<String, String> pMessage = pizzaMessage.produce_msg(faker, random, iterSeq);
			ProducerRecord<String, String> record = new ProducerRecord<String, String>(topicName, pMessage.get("key"), pMessage.get("message"));

			sendMessage(kafkaProducer, record, pMessage, sync);

			if ((intervalCount > 0) && (iterSeq % intervalCount == 0)) {
				try {
					logger.info("intervalMillis {}", intervalMillis);
					logger.info("intervalCount {}", intervalCount);
					Thread.sleep(intervalMillis);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}

			if (interIntervalMillis > 0) {
				try {
					logger.info("interIntervalMillis {}", interIntervalMillis);
					Thread.sleep(interIntervalMillis);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;

		logger.info("elapsedTime {} for {} iterations", elapsedTime, iterCount);
	}

	public static void sendMessage(KafkaProducer<String, String> producer, ProducerRecord<String, String> record, HashMap<String, String> pMessage,
			boolean sync) {

		if (!sync) {
			producer.send(record, (metadata, exception) -> {
				if (exception == null) {
					logger.info("async message: " + pMessage.get("key") + " partition: " + metadata.partition() + "\n" +
							"offset:" + metadata.offset());
				} else {
					logger.info("\n ###### Record metadata error ###### \n" +
							"exception: " + exception.getMessage());
				}
			});
		} else {
			try {
				RecordMetadata recordMetadata = producer.send(record).get();
				logger.info("sync message: " + pMessage.get("key") + "partition: " + recordMetadata.partition() + "\n" +
						"offset:" + recordMetadata.offset() + "\n" +
						"timestamp:" + recordMetadata.timestamp());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} finally {
				producer.close();
			}
		}
	}

	public static void main(String[] args) {

		String topic = "pizza-topic-partitioner";

		Properties props = new Properties();
		props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "dev-tools:9092,dev-tools:9093,dev-tools:9094");
		props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.setProperty("specialKey", "P001");
		props.setProperty(ProducerConfig.PARTITIONER_CLASS_CONFIG, "com.example.kafka.CustomPartitioner");

		KafkaProducer<String, String> producer = new KafkaProducer<>(props);

		sendPizzaMessage(producer, topic, -1, 10, 100, 100, false);

		producer.close();

	}
}
