package theater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import theater.config.kafka.KafkaProcessor;

@Service
public class PolicyHandler{
    @Autowired ApprovalRepository approvalRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_Cancel(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener Cancel : " + canceled.toJson() + "\n\n");

        // Sample Logic //
        Approval approval = new Approval();
        approvalRepository.save(approval);
            
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
