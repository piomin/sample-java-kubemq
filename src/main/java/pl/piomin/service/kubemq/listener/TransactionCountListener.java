package pl.piomin.service.kubemq.listener;

import io.grpc.stub.StreamObserver;
import io.kubemq.sdk.event.EventReceive;
import io.kubemq.sdk.tools.Converter;

public class TransactionCountListener implements StreamObserver<EventReceive> {

    @Override
    public void onNext(EventReceive eventReceive) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {

    }

}
