package com.redhat.mercury.myccr.services.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.mercury.myprp.services.impl.CustomerOfferEventHandler;
import com.redhat.mercury.partyroutingprofile.v10.RetrieveStatusResponseOuterClass.RetrieveStatusResponse;
import com.redhat.mercury.partyroutingprofile.v10.StatusOuterClass.Status;
import com.redhat.mercury.partyroutingprofile.v10.api.bqstatusservice.BqStatusService.RetrieveStatusRequest;
import com.redhat.mercury.partyroutingprofile.v10.client.PartyRoutingProfileClient;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class MyPRPServiceImplTest {

    @Inject
    PartyRoutingProfileClient client;

    @Inject
    CustomerOfferEventHandler eventHandler;

    @Test
    void testRetrievePartyStateStatus() throws ExecutionException, InterruptedException, TimeoutException {
        String prpId = "kermit";
        eventHandler.onCustomerOfferCompleted(prpId).await().indefinitely();
        CompletableFuture<RetrieveStatusResponse> message = new CompletableFuture<>();
        client.getBqStatusService().retrieveStatus(
                RetrieveStatusRequest.newBuilder()
                        .setPartyroutingprofileId(prpId)
                        .build()
        ).subscribe().with(message::complete);

        RetrieveStatusResponse expected = RetrieveStatusResponse.newBuilder()
                .setStatus(Status.newBuilder()
                        .setCustomerRelationshipStatus(prpId)
                        .build())
                .build();
        assertThat(message.get(5, TimeUnit.SECONDS)).isEqualTo(expected);
    }

}
