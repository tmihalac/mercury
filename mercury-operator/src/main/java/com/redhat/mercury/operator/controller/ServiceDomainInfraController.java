package com.redhat.mercury.operator.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.mercury.operator.model.KafkaConfig;
import com.redhat.mercury.operator.model.MercuryConstants;
import com.redhat.mercury.operator.model.ServiceDomainInfra;
import com.redhat.mercury.operator.model.ServiceDomainInfraSpec;
import com.redhat.mercury.operator.model.ServiceDomainInfraStatus;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.strimzi.api.kafka.model.EntityOperatorSpecBuilder;
import io.strimzi.api.kafka.model.EntityTopicOperatorSpecBuilder;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaBuilder;
import io.strimzi.api.kafka.model.KafkaClusterSpecBuilder;
import io.strimzi.api.kafka.model.ZookeeperClusterSpecBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerBuilder;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.ListenerStatus;
import io.strimzi.api.kafka.model.storage.EphemeralStorage;
import io.strimzi.api.kafka.model.storage.PersistentClaimStorageBuilder;
import io.strimzi.api.kafka.model.storage.SingleVolumeStorage;
import io.strimzi.api.kafka.model.storage.Storage;

import static com.redhat.mercury.operator.model.AbstractResourceStatus.MESSAGE_WAITING;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.REASON_WAITING;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.STATUS_FALSE;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.STATUS_TRUE;
import static com.redhat.mercury.operator.model.ServiceDomainInfraStatus.CONDITION_KAFKA_BROKER_READY;
import static com.redhat.mercury.operator.model.ServiceDomainInfraStatus.CONDITION_READY;
import static com.redhat.mercury.operator.model.ServiceDomainInfraStatus.MESSAGE_KAFKA_BROKER_NOT_READY;
import static com.redhat.mercury.operator.model.ServiceDomainInfraStatus.REASON_KAFKA_EXCEPTION;
import static com.redhat.mercury.operator.model.ServiceDomainInfraStatus.REASON_KAFKA_WAITING;

@ControllerConfiguration
public class ServiceDomainInfraController extends AbstractController<ServiceDomainInfraSpec, ServiceDomainInfraStatus, ServiceDomainInfra> implements Reconciler<ServiceDomainInfra>, EventSourceInitializer<ServiceDomainInfra> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDomainInfraController.class);

    private static final String DEFAULT_PERSISTENT_STORAGE = "100Gi";
    public static final String KAFKA_LISTENER_TYPE_PLAIN = "plain";
    private static final String KAFKA_VERSION = "3.0.0";
    private static final String BROKER_PROTOCOL_VERSION = "3.0";

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<ServiceDomainInfra> context) {
        SharedIndexInformer<Kafka> kafkaInformer = client.resources(Kafka.class)
                .inAnyNamespace()
                .withLabel(MANAGED_BY_LABEL, OPERATOR_NAME)
                .runnableInformer(0);

        return List.of(getInformerEventSource(kafkaInformer));
    }

    @Override
    public UpdateControl<ServiceDomainInfra> reconcile(ServiceDomainInfra sdi, Context context) {
        setStatusCondition(sdi, new ConditionBuilder()
                .withType(CONDITION_READY)
                .withStatus(STATUS_FALSE)
                .withReason(REASON_WAITING)
                .withMessage(MESSAGE_WAITING)
                .build());
        try {
            Condition kafkaCondition = createOrUpdateKafkaBroker(sdi);
            if (kafkaCondition != null) {
                return updateStatusWithCondition(sdi, kafkaCondition);
            }
            kafkaCondition = setKafkaBrokerUrl(sdi);
            if (STATUS_FALSE.equals(kafkaCondition.getStatus())) {
                return updateStatusWithCondition(sdi, kafkaCondition);
            }
            setStatusCondition(sdi, kafkaCondition);
            if (areAllConditionsReady(sdi)) {
                return updateStatusWithCondition(sdi, buildReadyCondition(CONDITION_READY));
            }
            return updateStatus(sdi);
        } catch (Exception e) {
            LOGGER.error("{} service domain infra failed to be created/updated", sdi.getMetadata().getName(), e);
            return updateStatusWithCondition(sdi, new ConditionBuilder()
                    .withType(CONDITION_KAFKA_BROKER_READY)
                    .withStatus(STATUS_FALSE)
                    .withReason(REASON_KAFKA_EXCEPTION)
                    .withMessage(e.getMessage())
                    .build());
        }
    }

    private Condition setKafkaBrokerUrl(ServiceDomainInfra sdi) {
        Kafka kafka = client.resources(Kafka.class)
                .inNamespace(sdi.getMetadata().getNamespace())
                .withName(sdi.getMetadata().getName())
                .get();

        if (isKafkaBrokerReady(kafka)) {
            LOGGER.debug("KafkaBroker for {} is Ready", sdi.getMetadata().getName());
            List<ListenerStatus> listeners = kafka.getStatus().getListeners();
            Optional<ListenerStatus> listenerStatus = listeners.stream()
                    .filter(x -> KAFKA_LISTENER_TYPE_PLAIN.equals(x.getType()))
                    .findFirst();
            if (listenerStatus.isPresent()) {
                LOGGER.debug("Assigning Kafka bootstrapServer with value {} to {}",
                        listenerStatus.get().getBootstrapServers(), sdi.getMetadata().getName());
                sdi.getStatus().setKafkaBroker(listenerStatus.get().getBootstrapServers());
            }
            return new ConditionBuilder()
                    .withType(CONDITION_KAFKA_BROKER_READY)
                    .withStatus(STATUS_TRUE)
                    .build();
        }
        LOGGER.debug("KafkaBroker for {} is not yet Ready", sdi.getMetadata().getName());
        return new ConditionBuilder()
                .withType(CONDITION_KAFKA_BROKER_READY)
                .withStatus(STATUS_FALSE)
                .withReason(REASON_KAFKA_WAITING)
                .withMessage(MESSAGE_KAFKA_BROKER_NOT_READY)
                .build();
    }

    private boolean isKafkaBrokerReady(Kafka kafka) {
        if (kafka == null || kafka.getStatus() == null || kafka.getStatus().getConditions() == null) {
            return false;
        }
        Optional<io.strimzi.api.kafka.model.status.Condition> condition = kafka.getStatus()
                .getConditions()
                .stream()
                .filter(c -> c.getType().equals(CONDITION_READY))
                .findFirst();
        return condition.isPresent() && condition.get().getStatus().equals(STATUS_TRUE);
    }

    private Condition createOrUpdateKafkaBroker(ServiceDomainInfra sdi) {
        final String sdiName = sdi.getMetadata().getName();
        Kafka desiredKafka = createKafkaObj(sdi);
        Kafka currentKafka = client.resources(Kafka.class)
                .inNamespace(sdi.getMetadata().getNamespace())
                .withName(sdiName)
                .get();

        if (currentKafka == null || !Objects.equals(currentKafka.getSpec(), desiredKafka.getSpec())) {
            LOGGER.debug("Creating or replacing Kafka {}", desiredKafka);
            currentKafka = client.resources(Kafka.class).inNamespace(sdi.getMetadata().getNamespace()).createOrReplace(desiredKafka);
            LOGGER.debug("Created or replaced Kafka {}", currentKafka);
            return new ConditionBuilder()
                    .withType(CONDITION_KAFKA_BROKER_READY)
                    .withStatus(STATUS_FALSE)
                    .withReason(REASON_KAFKA_WAITING)
                    .withMessage(MESSAGE_KAFKA_BROKER_NOT_READY)
                    .build();
        }
        LOGGER.debug("Kafka {} was not updated", sdiName);
        return null;
    }

    protected Kafka createKafkaObj(ServiceDomainInfra sdi) {
        Kafka desiredKafka = new KafkaBuilder()
                .withNewMetadata()
                .withName(sdi.getMetadata().getName())
                .withNamespace(sdi.getMetadata().getNamespace())
                .withLabels(Map.of(MANAGED_BY_LABEL, OPERATOR_NAME))
                .endMetadata()
                .withNewSpec()
                .withEntityOperator(new EntityOperatorSpecBuilder()
                        .withTopicOperator(new EntityTopicOperatorSpecBuilder().build())
                        .build())
                .withKafka(new KafkaClusterSpecBuilder()
                        .withReplicas(sdi.getSpec().getKafka().getReplicas())
                        .withListeners(new GenericKafkaListenerBuilder()
                                        .withName("plain")
                                        .withPort(9092)
                                        .withType(KafkaListenerType.INTERNAL)
                                        .withTls(false)
                                        .build(),
                                new GenericKafkaListenerBuilder()
                                        .withName("tls")
                                        .withPort(9093)
                                        .withType(KafkaListenerType.INTERNAL)
                                        .withTls(true)
                                        .build())
                        .withVersion(KAFKA_VERSION)
                        .withConfig(Map.of(
                                "inter.broker.protocol.version", BROKER_PROTOCOL_VERSION,
                                "default.replication.factor", sdi.getSpec().getKafka().getReplicas(),
                                "offsets.topic.replication.factor", sdi.getSpec().getKafka().getReplicas(),
                                "transaction.state.log.replication.factor", sdi.getSpec().getKafka().getReplicas(),
                                "transaction.state.log.min.isr", getMinIsr(sdi.getSpec().getKafka().getReplicas()),
                                "min.insync.replicas", getMinIsr(sdi.getSpec().getKafka().getReplicas())))
                        .withStorage(buildKafkaStorage(sdi.getSpec().getKafka()))
                        .build())
                .withZookeeper(new ZookeeperClusterSpecBuilder()
                        .withReplicas(sdi.getSpec().getKafka().getReplicas())
                        .withStorage(buildKafkaStorage(sdi.getSpec().getKafka()))
                        .build())
                .endSpec()
                .build();

        desiredKafka.getMetadata().setOwnerReferences(List.of(new OwnerReferenceBuilder()
                .withName(sdi.getMetadata().getName())
                .withUid(sdi.getMetadata().getUid())
                .withKind(ServiceDomainInfra.class.getSimpleName())
                .withApiVersion(MercuryConstants.API_VERSION)
                .build()));
        return desiredKafka;
    }

    private int getMinIsr(Integer replicas) {
        return replicas / 2 + 1;
    }

    private SingleVolumeStorage buildKafkaStorage(KafkaConfig config) {
        switch (config.getStorage().getType()) {
            case Storage.TYPE_EPHEMERAL:
                return new EphemeralStorage();
            case Storage.TYPE_PERSISTENT_CLAIM:
                Optional<String> size = Optional.of(config.getStorage().getSize());
                return new PersistentClaimStorageBuilder()
                        .withSize(size.orElse(DEFAULT_PERSISTENT_STORAGE))
                        .build();
            default:
                throw new IllegalArgumentException("Unsupported kafka storage type: "
                        + config.getStorage().getType()
                        + " supported values are 'ephemeral' and 'persistent-volume-claim'");
        }
    }
}
