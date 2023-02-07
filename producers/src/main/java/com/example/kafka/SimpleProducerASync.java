package com.example.kafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class SimpleProducerASync {

	public static final Logger logger = Logger.getLogger(SimpleProducerASync.class.getName());

	public static void main(String[] args) {

		String topic = "simple-producer";

		Properties props = new Properties();
		props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "dev-tools:9092,dev-tools:9093,dev-tools:9094");
		props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

		KafkaProducer<String, String> producer = new KafkaProducer<>(props);

		ProducerRecord<String, String> record = new ProducerRecord<>(topic, "hello world");

		producer.send(record, new Callback() {
			@Override
			public void onCompletion(RecordMetadata metadata, Exception exception) {
				if (exception == null) {
					logger.info("\n ###### Record metadata received ###### \n" +
							"partition: " + metadata.partition() + "\n" +
							"offset:" + metadata.offset() + "\n" +
							"timestamp:" + metadata.timestamp());
				} else {
					logger.info("\n ###### Record metadata error ###### \n" +
							"exception: " + exception.getMessage());
				}
			}
		});

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		producer.close();

	}
}
