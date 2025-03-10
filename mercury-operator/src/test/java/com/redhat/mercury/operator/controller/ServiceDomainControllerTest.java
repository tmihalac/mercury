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
import com.redhat.mercury.operator.model.ServiceDomainInfra;
import com.redhat.mercury.operator.model.ServiceDomainSpec;
import com.redhat.mercury.operator.utils.ResourceUtils;

import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaTopic;
import io.strimzi.api.kafka.model.status.ConditionBuilder;
import io.strimzi.api.kafka.model.status.KafkaTopicStatusBuilder;

import static com.redhat.mercury.operator.controller.ServiceDomainController.INTEGRATION_SUFFIX;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.CONDITION_READY;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.MESSAGE_WAITING;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.REASON_FAILED;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.REASON_WAITING;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.STATUS_FALSE;
import static com.redhat.mercury.operator.model.AbstractResourceStatus.STATUS_TRUE;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.CONDITION_INTEGRATION_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.CONDITION_KAFKA_TOPIC_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.CONDITION_SERVICE_DOMAIN_INFRA_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.MESSAGE_CONFIG_MAP_MISSING;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.MESSAGE_SDI_NOT_FOUND;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.MESSAGE_SDI_NOT_READY;
import static com.redhat.mercury.operator.model.ServiceDomainStatus.REASON_SDI;
import static com.redhat.mercury.operator.utils.ResourceUtils.toLowerHyphen;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithKubernetesTestServer
public class ServiceDomainControllerTest extends AbstractControllerTest {

    @BeforeEach
    public void beforeEach() throws IOException {
        mockServer.getKubernetesMockServer().clearExpectations();

        createDefultNamespace();
        createDefaultIntegrationConfigMap();
        createOpenApiConfigMap();
        createDefaultKafka();
    }

    @AfterEach
    public void afterEach() {
        mockServer.getKubernetesMockServer().clearExpectations();

        deleteDeployment();
        deleteService();
        deleteKafkaTopic();
        deleteIntegrationConfigMap();
        deleteOpenAPIConfigMap();
        deleteIntegration();
        deleteServiceDomains();
        deleteKafka();
    }

    @Test
    public void testAddServiceDomain() {
        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNotNull();
        assertOwnerReference(sd, integration.getMetadata().getOwnerReferences());

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(4);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertThat(kafkaTopic).isNotNull();
        assertOwnerReference(sd, kafkaTopic.getMetadata().getOwnerReferences());

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsReady(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(4);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(deployment).isNotNull();
        assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(service).isNotNull();
        assertOwnerReference(sd, service.getMetadata().getOwnerReferences());
    }

    @Test
    public void testAddServiceDomainNoConfigMaps() {
        deleteIntegrationConfigMap();

        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNull();

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition).isNull();

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(deployment).isNotNull();
        assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(service).isNotNull();
        assertOwnerReference(sd, service.getMetadata().getOwnerReferences());
    }

    @Test
    public void testAddServiceDomainBadIntegrationConfigMap() throws IOException {
        deleteIntegrationConfigMap();
        createIntegrationConfigMap("badSdConfigMap.yaml");

        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNull();

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition).isNull();

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(deployment).isNotNull();
        assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(service).isNotNull();
        assertOwnerReference(sd, service.getMetadata().getOwnerReferences());
    }

    @Test
    public void testAddServiceDomainWithNoOpenApiConfigMap(){
        deleteOpenAPIConfigMap();

        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain();
        final ServiceDomainSpec.Type sdType = sd.getSpec().getType();
        final String sdTypeAsString = toLowerHyphen(sdType.toString());
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getMessage()).isEqualTo(sdTypeAsString + "-openapi" + " " + MESSAGE_CONFIG_MAP_MISSING);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition).isNull();

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(deployment).isNotNull();
        assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(service).isNotNull();
        assertOwnerReference(sd, service.getMetadata().getOwnerReferences());
    }

    @Test
    void testAddSDWithException() {
        ServiceDomain sd = createServiceDomain();
        ServiceDomainInfra sdi = createReadySDI();

        String exceptionMessage = "Test exception";
        mockServer.expect().get()
                .withPath("/apis/apps/v1/namespaces/test-service-domain/deployments/customer-offer")
                .andReturn(500, new KubernetesClientException(exceptionMessage))
                .always();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThat(update.isUpdateStatus()).isTrue();
        assertThat(update.getResource().getStatus().isReady()).isFalse();
        assertThat(update.getResource().getStatus().getConditions()).hasSize(2);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_READY);
        assertThat(condition.getStatus()).isEqualTo(ResourceUtils.toStatus(Boolean.FALSE));
        assertThat(condition.getReason()).isEqualTo(REASON_FAILED);
        assertThat(condition.getMessage()).contains(exceptionMessage);
        assertThat(condition.getLastTransitionTime()).isNotNull();
    }

    @Test
    public void testUpdateServiceDomainToNoExpose() {
        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNotNull();
        assertOwnerReference(sd, integration.getMetadata().getOwnerReferences());

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(4);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertThat(kafkaTopic).isNotNull();
        assertOwnerReference(sd, kafkaTopic.getMetadata().getOwnerReferences());

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsReady(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(4);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(deployment).isNotNull();
        assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(service).isNotNull();
        assertOwnerReference(sd, service.getMetadata().getOwnerReferences());

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsReady(update);

        update.getResource().getSpec().setExpose(null);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsReady(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition).isNull();

        integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNull();
    }

    @Test
    public void testAddServiceDomainWithNoInfra() {
        ServiceDomain sd = createServiceDomain();
        final String sdiName = sd.getSpec().getServiceDomainInfra();

        final UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(2);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);
        assertThat(condition.getReason()).isEqualTo(REASON_SDI);
        assertThat(condition.getMessage()).isEqualTo(sdiName + " " + MESSAGE_SDI_NOT_FOUND);
    }

    @Test
    public void testAddServiceDomainWithInfraNotReady() {
        ServiceDomainInfra sdi = createNotReadySDI();
        final String sdiName = sdi.getMetadata().getName();
        ServiceDomain sd = createServiceDomain();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        final UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(2);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);
        assertThat(condition.getReason()).isEqualTo(REASON_SDI);
        assertThat(condition.getMessage()).isEqualTo(sdiName + " " + MESSAGE_SDI_NOT_READY);
    }

    @Test
    public void testAddServiceDomainWithoutExposeHttp() {
        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain(SERVICE_DOMAIN_NAME, false);
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
        assertThatIsWaiting(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);
        condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
        assertThat(condition).isNull();
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

        //Test deployment data
        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(deployment).isNotNull();
        assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());

        //Test Service data
        final Service service = client.services().inNamespace(sdNamespace).withName(sdName).get();
        assertThat(service).isNotNull();
        assertOwnerReference(sd, service.getMetadata().getOwnerReferences());

        //Test Integration data
        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNull();

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertThat(kafkaTopic).isNotNull();
        assertOwnerReference(sd, kafkaTopic.getMetadata().getOwnerReferences());

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);

        update = serviceDomainController.reconcile(update.getResource(), null);
        assertThatIsReady(update);
        assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
        condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
        assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);
    }

    @Test
    public void testAddMultipleServiceDomain() {
        ServiceDomainInfra sdi = createReadySDI();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        for (int i = 0; i < 3; i++) {
            ServiceDomain sd = createServiceDomain();
            final String sdNamespace = sd.getMetadata().getNamespace();
            final String sdName = sd.getMetadata().getName() + i;
            sd.getMetadata().setName(sdName);
            final NamespacedKubernetesClient client = mockServer.getClient();

            UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);
            assertThatIsWaiting(update);
            assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
            Condition condition = update.getResource().getStatus().getCondition(CONDITION_SERVICE_DOMAIN_INFRA_READY);
            assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

            update = serviceDomainController.reconcile(update.getResource(), null);
            assertThatIsWaiting(update);
            assertThat(update.getResource().getStatus().getConditions()).hasSize(3);
            condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
            assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

            final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
            ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                    .withGroup("camel.apache.org")
                    .withVersion("v1")
                    .withPlural("integrations")
                    .withNamespaced(true)
                    .build();

            final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
            assertThat(integration).isNotNull();
            assertOwnerReference(sd, integration.getMetadata().getOwnerReferences());

            integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
            client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);

            update = serviceDomainController.reconcile(update.getResource(), null);
            assertThatIsWaiting(update);
            assertThat(update.getResource().getStatus().getConditions()).hasSize(4);
            condition = update.getResource().getStatus().getCondition(CONDITION_INTEGRATION_READY);
            assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);
            condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
            assertThat(condition.getStatus()).isEqualTo(STATUS_FALSE);

            KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
            assertThat(kafkaTopic).isNotNull();
            assertOwnerReference(sd, kafkaTopic.getMetadata().getOwnerReferences());

            kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
            client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);

            update = serviceDomainController.reconcile(update.getResource(), null);
            assertThatIsReady(update);
            assertThat(update.getResource().getStatus().getConditions()).hasSize(4);
            condition = update.getResource().getStatus().getCondition(CONDITION_KAFKA_TOPIC_READY);
            assertThat(condition.getStatus()).isEqualTo(STATUS_TRUE);

            //Test deployment data
            final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(sdName).get();
            assertThat(deployment).isNotNull();
            assertOwnerReference(sd, deployment.getMetadata().getOwnerReferences());
        }
    }

    @Test
    public void testWatchDeletedObjectsTest() {
        ServiceDomainInfra sdi = createReadySDI();
        ServiceDomain sd = createServiceDomain();
        final String sdNamespace = sd.getMetadata().getNamespace();
        final String sdName = sd.getMetadata().getName();
        final NamespacedKubernetesClient client = mockServer.getClient();

        mockServer.expect().get()
                .withPath("/apis/mercury.redhat.io/v1alpha1/namespaces/test-service-domain/servicedomaininfras/service-domain-infra")
                .andReturn(200, sdi)
                .always();

        UpdateControl<ServiceDomain> update = serviceDomainController.reconcile(sd, null);

        update = serviceDomainController.reconcile(update.getResource(), null);

        update = serviceDomainController.reconcile(update.getResource(), null);

        final String integrationName = sd.getMetadata().getName() + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();

        integration.getAdditionalProperties().put("status", Map.of("conditions", List.of(Map.of("type", "Ready", "status", "True"))));
        client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).replace(integration);

        update = serviceDomainController.reconcile(update.getResource(), null);

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();

        kafkaTopic.setStatus(new KafkaTopicStatusBuilder().withConditions(new ConditionBuilder().withType(CONDITION_READY).withStatus("True").build()).build());
        client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").replace(kafkaTopic);

        serviceDomainController.reconcile(update.getResource(), null);

        Boolean deleted = client.apps().deployments().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).delete();
        assertThat(deleted).isTrue();

        final Deployment deployment = client.apps().deployments().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get();
        assertThat(deployment).isNull();

        deleted = client.services().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).delete();
        assertThat(deleted).isTrue();

        final Service service = client.services().inNamespace(sdNamespace).withName(SERVICE_DOMAIN_NAME).get();
        assertThat(service).isNull();

        deleted = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).delete();
        assertThat(deleted).isTrue();

        integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(sdNamespace).withName(integrationName).get();
        assertThat(integration).isNull();

        deleted = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").delete();
        assertThat(deleted).isTrue();

        kafkaTopic = client.resources(KafkaTopic.class).inNamespace(sdNamespace).withName(sdName + "-topic").get();
        assertThat(kafkaTopic).isNull();
    }

    private void assertOwnerReference(ServiceDomain sd, List<OwnerReference> ownerReferences) {
        assertThat(ownerReferences).isNotNull();
        assertThat(ownerReferences).isNotEmpty();

        OwnerReference ownerReference = ownerReferences.get(0);
        assertThat(ownerReference).isNotNull();
        assertThat(sd.getMetadata().getName()).isEqualTo(ownerReference.getName());
        assertThat(ServiceDomainController.SERVICE_DOMAIN_OWNER_REFERENCES_KIND).isEqualTo(ownerReference.getKind());
        assertThat(MercuryConstants.API_VERSION).isEqualTo(ownerReference.getApiVersion());
    }

    private void createDefultNamespace() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final Namespace namespace = client.namespaces().withName(SERVICE_DOMAIN_INFRA_NAMESPACE).get();
        if (namespace == null) {
            client.namespaces().create(new NamespaceBuilder().withNewMetadata().withName(SERVICE_DOMAIN_INFRA_NAMESPACE).endMetadata().build());
        }
    }

    private void createDefaultKafka() {
        final NamespacedKubernetesClient client = mockServer.getClient();
        //Hack so that the infra will already have a kafka broker
        ServiceDomainInfra sdi = createReadySDI();

        Kafka expectedKafka = getExpectedKafKa(sdi);

        Kafka fetchedKafka = client.resources(Kafka.class).inNamespace(sdi.getMetadata().getNamespace()).withName(SERVICE_DOMAIN_INFRA_NAME).get();
        if (fetchedKafka == null) {
            client.resources(Kafka.class).inNamespace(sdi.getMetadata().getNamespace()).create(expectedKafka);
            fetchedKafka = client.resources(Kafka.class).inNamespace(sdi.getMetadata().getNamespace()).withName(SERVICE_DOMAIN_INFRA_NAME).get();
            assertThat(fetchedKafka).isNotNull();
        }
    }

    private void createOpenApiConfigMap() {
        final NamespacedKubernetesClient client = mockServer.getClient();
        ConfigMap openAPIConfigMap = client.configMaps()
                .inNamespace(client.getNamespace())
                .load(this.getClass().getClassLoader().getResourceAsStream("openAPIConfigMap.yaml"))
                .create();
        assertThat(openAPIConfigMap).isNotNull();
    }

    private void createDefaultIntegrationConfigMap() throws IOException {
        createIntegrationConfigMap("sdConfigMap.yaml");
    }

    private void createIntegrationConfigMap(String fileName) throws IOException {
        final NamespacedKubernetesClient client = mockServer.getClient();
        final String sdConfigMapName = "integration-" + SERVICE_DOMAIN_NAME + "-http";
        ConfigMap configMap = client.configMaps().inNamespace(client.getNamespace()).withName(sdConfigMapName).get();
        if (configMap == null) {
            try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (inputStream != null) {
                    client.configMaps().inNamespace(client.getNamespace()).load(inputStream).create();
                    configMap = client.configMaps().inNamespace(client.getNamespace()).withName(sdConfigMapName).get();
                    assertThat(configMap).isNotNull();
                }
            }
        }
    }

    private void deleteKafka() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final Kafka kafka = client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).withName(SERVICE_DOMAIN_INFRA_NAME).get();
        if (kafka != null) {
            client.resources(Kafka.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).delete(kafka);
        }
    }

    private void deleteServiceDomains() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final KubernetesResourceList<ServiceDomain> sdList = client.resources(ServiceDomain.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).list();
        if (sdList != null && sdList.getItems() != null && !sdList.getItems().isEmpty()) {
            for (ServiceDomain sd : sdList.getItems()) {
                client.resources(ServiceDomain.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).delete(sd);
                assertThat(client.resources(ServiceDomain.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).withName(sd.getMetadata().getName()).get()).isNull();
            }
        }
    }

    private void deleteIntegration() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final String integrationName = SERVICE_DOMAIN_NAME + INTEGRATION_SUFFIX;
        ResourceDefinitionContext resourceDefinitionContext = new ResourceDefinitionContext.Builder()
                .withGroup("camel.apache.org")
                .withVersion("v1")
                .withPlural("integrations")
                .withNamespaced(true)
                .build();

        final GenericKubernetesResource integration = client.genericKubernetesResources(resourceDefinitionContext).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).withName(integrationName).get();
        if (integration != null) {
            client.genericKubernetesResources(resourceDefinitionContext).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).delete(integration);
        }
    }

    private void deleteIntegrationConfigMap() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final String sdConfigMapName = "integration-" + SERVICE_DOMAIN_NAME + "-http";
        final ConfigMap configMap = client.configMaps().inNamespace(client.getNamespace()).withName(sdConfigMapName).get();
        if (configMap != null) {
            client.configMaps().inNamespace(client.getNamespace()).delete(configMap);
        }
    }

    private void deleteOpenAPIConfigMap() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        final Resource<ConfigMap> configMapResource = client.configMaps().inNamespace(client.getNamespace()).withName("customer-offer-openapi");
        if(configMapResource != null) {
           configMapResource.delete();
        }
    }

    private void deleteKafkaTopic() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        KafkaTopic kafkaTopic = client.resources(KafkaTopic.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).withName(SERVICE_DOMAIN_NAME + "-topic").get();
        if (kafkaTopic != null) {
            client.resources(KafkaTopic.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).delete(kafkaTopic);
        }
    }

    private void deleteService() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        Service service = client.services().inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).withName(SERVICE_DOMAIN_NAME).get();
        if (service != null) {
            client.resources(Service.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).delete(service);
        }
    }

    private void deleteDeployment() {
        final NamespacedKubernetesClient client = mockServer.getClient();

        Deployment sdDeployment = client.apps().deployments().inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).withName(SERVICE_DOMAIN_NAME).get();
        if (sdDeployment != null) {
            client.resources(Deployment.class).inNamespace(SERVICE_DOMAIN_INFRA_NAMESPACE).delete(sdDeployment);
        }
    }
}
