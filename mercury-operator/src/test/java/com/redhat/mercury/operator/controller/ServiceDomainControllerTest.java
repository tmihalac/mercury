package com.redhat.mercury.operator.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.mercury.operator.model.MercuryConstants;
import com.redhat.mercury.operator.model.ServiceDomain;
import com.redhat.mercury.operator.model.ServiceDomainCluster;
import com.redhat.mercury.operator.model.ServiceDomainClusterSpecBuilder;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.api.kafka.model.status.KafkaTopicStatusBuilder;

import static com.redhat.mercury.operator.controller.ServiceDomainController.INTEGRATION_SUFFIX;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.CONDITION_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.CONDITION_INTEGRATION_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.CONDITION_KAFKA_TOPIC_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.CONDITION_SERVICE_DOMAIN_CLUSTER_READY;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@WithKubernetesTestServer
public class ServiceDomainControllerTest extends AbstractControllerTest {

    @BeforeEach
    public void beforeEach() throws IOException {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final Namespace namespace = client.namespaces().withName(SERVICE_DOMAIN_CLUSTER_NAMESPACE).get();
        if (namespace == null) {
            client.namespaces().create(new NamespaceBuilder().withNewMetadata().withName(SERVICE_DOMAIN_CLUSTER_NAMESPACE).endMetadata().build());
        }

        final String sdConfigMapName = "integration-" + SERVICE_DOMAIN_NAME + "-http";
        ConfigMap configMap = client.configMaps().inNamespace(client.getNamespace()).withName(sdConfigMapName).get();
        if (configMap == null) {
            try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sdConfigMap.yaml")) {
                if (inputStream != null) {
                    client.configMaps().inNamespace(client.getNamespace()).load(inputStream).create();
                    assertNotNull(client.configMaps().inNamespace(client.getNamespace()).withName(sdConfigMapName).get());
                }
            }
        }

        String openApiConfigMap = "customer-offer-openapi";
        ConfigMap openAPIConfigMap = client.configMaps()
                .inNamespace(client.getNamespace())
                .load(this.getClass().getClassLoader().getResourceAsStream("openAPIConfigMap.yaml"))
                .create();
        assertNotNull(openAPIConfigMap);
        assertNotNull(client.configMaps().inNamespace(client.getNamespace()).withName(openApiConfigMap).get());

        //Hack so that the cluster will already have a kafka broker
        ServiceDomainCluster sdc = new ServiceDomainCluster();
        sdc.setMetadata(new ObjectMetaBuilder().withNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).build());
        sdc.setSpec(new ServiceDomainClusterSpecBuilder().build());
        Kafka expectedKafka = getExpectedKafKa(sdc);
        Kafka fetchedKafka = client.resources(Kafka.class).inNamespace(sdc.getMetadata().getNamespace()).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        if (fetchedKafka == null) {
            client.resources(Kafka.class).inNamespace(sdc.getMetadata().getNamespace()).create(expectedKafka);
            assertNotNull(client.resources(Kafka.class).inNamespace(sdc.getMetadata().getNamespace()).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());
        }

        ServiceDomainCluster fetchedCluster = client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        if (fetchedCluster == null) {
            ServiceDomainCluster desiredCluster = createServiceDomainCluster();
            fetchedCluster = client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).create(desiredCluster);
            serviceDomainClusterController.reconcile(fetchedCluster, null);
            assertNotNull(client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());
        }

        final String sdcNamespace = fetchedCluster.getMetadata().getNamespace();
        //update the kafka broker owner with the cluster uid
        expectedKafka = getExpectedKafKa(fetchedCluster);
        fetchedKafka = client.resources(Kafka.class).inNamespace(sdcNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        if (fetchedKafka != null) {
            client.resources(Kafka.class).inNamespace(sdcNamespace).replace(expectedKafka);
            serviceDomainClusterController.reconcile(fetchedCluster, null);
            assertNotNull(client.resources(Kafka.class).inNamespace(sdcNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());
        }
    }

    @AfterEach
    public void afterEach() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        Deployment sdDeployment = client.apps().deployments().inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_NAME).get();
        if (sdDeployment != null) {
            client.resources(Deployment.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(sdDeployment);
        }

        Service service = client.services().inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_NAME).get();
        if (service != null) {
            client.resources(Service.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(service);
        }

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_NAME + "-topic").get();
        if (kafkaTopic != null) {
            client.resources(KafkaTopic.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(kafkaTopic);
        }

        final String sdConfigMapName = "integration-" + SERVICE_DOMAIN_NAME + "-http";
        final ConfigMap configMap = client.configMaps().inNamespace(client.getNamespace()).withName(sdConfigMapName).get();
        if (configMap != null) {
            client.configMaps().inNamespace(client.getNamespace()).delete(configMap);
        }
        client.configMaps().inNamespace(client.getNamespace()).withName("customer-offer-openapi").delete();

        final String integrationName = SERVICE_DOMAIN_NAME + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(integrationName).get();
        if (integration != null) {
            client.genericKubernetesResources(resourceDefinitionContext).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(integration);
        }

        final KubernetesResourceList<ServiceDomain> sdList = client.resources(ServiceDomain.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).list();
        if (sdList != null && sdList.getItems() != null && !sdList.getItems().isEmpty()) {
            for (ServiceDomain sd : sdList.getItems()) {
                client.resources(ServiceDomain.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(sd);
                assertNull(client.resources(ServiceDomain.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(sd.getMetadata().getName()).get());
            }
        }

        ServiceDomainCluster fetchedCluster = client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        if (fetchedCluster != null) {
            client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(fetchedCluster);
        }

        final Kafka kafka = client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        if (kafka != null) {
            client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(kafka);
        }
    }

    @Test
    public void addServiceDomainTest() {
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        sd = client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertNotNull(deployment);

        List<OwnerReference> ownerReferences = deployment.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        OwnerReference ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertNotNull(service);

        ownerReferences = service.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        //Test Kafka Broker data
        assertTrue(isServiceDomainClusterStatusUpdatedWithKafkaBrokerUrl(SERVICE_DOMAIN_CLUSTER_NAME));
        final String kafkaBrokerUrl = client.resources(ServiceDomainCluster.class).inNamespace(sdNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).get().getStatus().getKafkaBroker();
        assertNotNull(kafkaBrokerUrl);

        //Test Integration data
        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertNotNull(integration);

        ownerReferences = integration.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);
        serviceDomainController.reconcile(sd, null);

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertNotNull(kafkaTopic);

        ownerReferences = kafkaTopic.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);
        serviceDomainController.reconcile(sd, null);

        assertTrue(client.resources(ServiceDomain.class)
                .inNamespace(sdNamespace)
                .withName(sdName)
                .get().getStatus().isReady());
        ServiceDomain serviceDomain = client.resources(ServiceDomain.class)
                    .inNamespace(sdNamespace)
                    .withName(sdName)
                    .get();

        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_KAFKA_TOPIC_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_INTEGRATION_READY));
    }

    @Test
    public void updateServiceDomainNoExposeTest() {
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        sd = client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertNotNull(deployment);

        List<OwnerReference> ownerReferences = deployment.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        OwnerReference ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertNotNull(service);

        ownerReferences = service.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        //Test Kafka Broker data
        assertTrue(isServiceDomainClusterStatusUpdatedWithKafkaBrokerUrl(SERVICE_DOMAIN_CLUSTER_NAME));
        final String kafkaBrokerUrl = client.resources(ServiceDomainCluster.class).inNamespace(sdNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).get().getStatus().getKafkaBroker();
        assertNotNull(kafkaBrokerUrl);

        //Test Integration data
        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertNotNull(integration);

        ownerReferences = integration.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);
        final Object status = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get().getAdditionalProperties().get("status");
        assertNotNull(status);
        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertNotNull(kafkaTopic);

        ownerReferences = kafkaTopic.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);
        serviceDomainController.reconcile(sd, null);

       assertTrue(client.resources(ServiceDomain.class)
                .inNamespace(sdNamespace)
                .withName(sdName)
                .get().getStatus().isReady());
        ServiceDomain serviceDomain = client.resources(ServiceDomain.class)
                    .inNamespace(sdNamespace)
                    .withName(sdName)
                    .get();

        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_KAFKA_TOPIC_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_INTEGRATION_READY));

        sd.getSpec().setExpose(null);
        client.resources(ServiceDomain.class).inNamespace(sdNamespace).replace(sd);
        serviceDomainController.reconcile(sd, null);

        integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertNull(integration);

        assertTrue(client.resources(ServiceDomain.class)
                .inNamespace(sdNamespace)
                .withName(sdName)
                .get().getStatus().isReady());
        serviceDomain = client.resources(ServiceDomain.class)
                    .inNamespace(sdNamespace)
                    .withName(sdName)
                    .get();

        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_KAFKA_TOPIC_READY));
    }

    @Test
    public void addServiceDomainWithNoClusterTest() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final ServiceDomainCluster serviceDomainCluster = client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).delete();
        serviceDomainClusterController.reconcile(serviceDomainCluster, null);
        assertNull(client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());

        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        assertFalse(client.resources(ServiceDomain.class).inNamespace(sdNamespace).withName(sdName).get().getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
    }

    @Test
    public void addServiceDomainWithNoKafkaBrokerUrlTest() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final ServiceDomainCluster serviceDomainCluster = client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).delete();
        serviceDomainClusterController.reconcile(serviceDomainCluster, null);
        assertNull(client.resources(ServiceDomainCluster.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());

        final Kafka kafka = client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get();
        client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).delete(kafka);
        assertNull(client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_CLUSTER_NAMESPACE).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());

        ServiceDomainCluster desiredCluster = createServiceDomainCluster();
        final String sdcNamespace = desiredCluster.getMetadata().getNamespace();

        client.resources(ServiceDomainCluster.class).inNamespace(sdcNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).create(desiredCluster);
        serviceDomainClusterController.reconcile(desiredCluster, null);
        assertNotNull(client.resources(ServiceDomainCluster.class).inNamespace(sdcNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).get());

        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();

        client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        assertFalse(client.resources(ServiceDomain.class).inNamespace(sdNamespace).withName(sdName).get().getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
    }

    @Test
    public void addServiceDomainWithoutExposeHttpTest() {
        ServiceDomain sd = createServiceDomain(SERVICE_DOMAIN_NAME, false);
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        sd = client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get();
        assertNotNull(deployment);

        List<OwnerReference> ownerReferences = deployment.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        OwnerReference ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get();
        assertNotNull(service);

        ownerReferences = service.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        //Test Kafka Broker data
        assertTrue(isServiceDomainClusterStatusUpdatedWithKafkaBrokerUrl(SERVICE_DOMAIN_CLUSTER_NAME));
        final String kafkaBrokerUrl = client.resources(ServiceDomainCluster.class).inNamespace(sdNamespace).withName(SERVICE_DOMAIN_CLUSTER_NAME).get().getStatus().getKafkaBroker();
        assertNotNull(kafkaBrokerUrl);

        //Test Integration data
        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertNull(integration);

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertNotNull(kafkaTopic);

        ownerReferences = kafkaTopic.getMetadata().getOwnerReferences();
        assertNotNull(ownerReferences);
        assertFalse(ownerReferences.isEmpty());

        ownerReference = ownerReferences.get(0);
        assertNotNull(ownerReference);
        assertEquals(sd.getMetadata().getName(), ownerReference.getName());
        assertEquals(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND, ownerReference.getKind());
        assertEquals(MercuryConstants.API_VERSION, ownerReference.getApiVersion());

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);
        serviceDomainController.reconcile(sd, null);

        assertTrue(client.resources(ServiceDomain.class)
                .inNamespace(sdNamespace)
                .withName(sdName)
                .get().getStatus().isReady());
        ServiceDomain serviceDomain = client.resources(ServiceDomain.class)
                    .inNamespace(sdNamespace)
                    .withName(sdName)
                    .get();

        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_KAFKA_TOPIC_READY));
    }

    @Test
    public void addMultipleServiceDomainTest() {
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        Deployment sdDeployment = client.apps().deployments().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get();
        assertNotNull(sdDeployment);

        Service service = client.services().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get();
        assertNotNull(service);

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME + "-topic").get();
        assertNotNull(kafkaTopic);

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);

        //Test Integration data
        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertNotNull(integration);

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);

        assertTrue(client.resources(ServiceDomain.class)
                .inNamespace(sdNamespace)
                .withName(sdName)
                .get().getStatus().isReady());
        ServiceDomain serviceDomain = client.resources(ServiceDomain.class)
                    .inNamespace(sdNamespace)
                    .withName(sdName)
                    .get();

        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_KAFKA_TOPIC_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_INTEGRATION_READY));

        sd = createServiceDomain(sdName + 2);
        final String integrationName2 = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        final String sdName2 = sd.getMetadata().getName();
        client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        sdDeployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName2).get();
        assertNotNull(sdDeployment);

        service = client.services().inNamespace(sdNamespace).withName(sdName2).get();
        assertNotNull(service);

        kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName2 + "-topic").get();
        assertNotNull(kafkaTopic);

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName2 + "-topic").replace(kafkaTopic);

        //Test Integration data
        integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName2).get();
        assertNotNull(integration);

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName2).replace(integration);

        assertTrue(client.resources(ServiceDomain.class)
                .inNamespace(sdNamespace)
                .withName(sdName2)
                .get().getStatus().isReady());
        serviceDomain = client.resources(ServiceDomain.class)
                    .inNamespace(sdNamespace)
                    .withName(sdName2)
                    .get();

        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_SERVICE_DOMAIN_CLUSTER_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_KAFKA_TOPIC_READY));
        assertTrue(serviceDomain.getStatus().isSpecificConditionReady(CONDITION_INTEGRATION_READY));
    }

    @Test
    public void watchDeletedObjectsTest() {
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        sd = client.resources(ServiceDomain.class).inNamespace(sdNamespace).create(sd);
        serviceDomainController.reconcile(sd, null);

        Boolean deleted = client.apps().deployments().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).delete();
        assertTrue(deleted);

        assertNotNull(client.apps().deployments().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get());

        deleted = client.services().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).delete();
        assertTrue(deleted);

        assertNotNull(client.services().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get());

        //Test Integration data
        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        deleted = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).delete();
        assertTrue(deleted);

        assertNotNull(client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get());

        deleted = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").delete();
        assertTrue(deleted);

        assertNotNull(client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get());
    }
}
