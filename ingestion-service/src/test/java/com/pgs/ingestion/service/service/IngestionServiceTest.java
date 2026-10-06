package com.pgs.ingestion.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.pgs.ingestion.service.dto.EnergyUsageDto;
import com.pgs.kafka.event.EnergyUsageEvent;

/**
 * Unit tests for {@link IngestionService}. The {@link KafkaTemplate} is mocked
 * so no Kafka broker is required. We verify the DTO -> event mapping (the private
 * {@code toEvent} logic) through the captured event actually handed to Kafka, and
 * the behaviour of the completion callback on both success and failure.
 */
@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

	private static final String TOPIC = "energy-usage";

	@Mock
	private KafkaTemplate<String, EnergyUsageEvent> kafkaTemplate;

	@InjectMocks
	private IngestionService ingestionService;

	/* ------------------------------------------------------------------ */
	/*  Test data helpers                                                  */
	/* ------------------------------------------------------------------ */

	private static EnergyUsageDto sampleDto() {
		return EnergyUsageDto.builder()
				.deviceId(42L)
				.energyConsumed(3.5)
				.timestamp(Instant.parse("2026-10-04T15:30:45Z"))
				.build();
	}

	/* ------------------------------------------------------------------ */
	/*  ingestEnergyUsage (happy path)                                     */
	/* ------------------------------------------------------------------ */

	@Nested
	@DisplayName("ingestEnergyUsage")
	class IngestEnergyUsage {

		@Test
		void publishesEventWithMappedFieldsToEnergyUsageTopic() {
			when(kafkaTemplate.send(anyString(), any(EnergyUsageEvent.class)))
					.thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

			ingestionService.ingestEnergyUsage(sampleDto());

			ArgumentCaptor<EnergyUsageEvent> captor =
					ArgumentCaptor.forClass(EnergyUsageEvent.class);
			verify(kafkaTemplate).send(eq(TOPIC), captor.capture());

			EnergyUsageEvent event = captor.getValue();
			assertThat(event.deviceId()).isEqualTo(42L);
			assertThat(event.energyConsumed()).isEqualTo(3.5);
			assertThat(event.timestamp()).isEqualTo(Instant.parse("2026-10-04T15:30:45Z"));
		}

		@Test
		void swallowsFailureFromKafkaPublish() {
			// The future is completed exceptionally AFTER the send callback is
			// registered; the service's whenComplete must not propagate the error.
			CompletableFuture<SendResult<String, EnergyUsageEvent>> future =
					new CompletableFuture<>();
			when(kafkaTemplate.send(anyString(), any(EnergyUsageEvent.class)))
					.thenReturn(future);

			assertThatCode(() -> ingestionService.ingestEnergyUsage(sampleDto()))
					.doesNotThrowAnyException();

			future.completeExceptionally(new IllegalStateException("kafka down"));

			verify(kafkaTemplate).send(anyString(), any(EnergyUsageEvent.class));
		}
	}
}