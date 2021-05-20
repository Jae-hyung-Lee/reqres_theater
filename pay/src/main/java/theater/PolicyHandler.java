package theater;

import theater.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired ApprovalRepository approvalRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_Cancel_1(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener Cancel : " + canceled.toJson() + "\n\n");

        // Sample Logic //
        Approval approval = new Approval();
        approvalRepository.save(approval);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_Cancel_2(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener Cancel : " + canceled.toJson() + "\n\n");

        // Sample Logic //
        Approval approval = new Approval();
        approvalRepository.save(approval);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceled_Cancel_3(@Payload Canceled canceled){

        if(!canceled.validate()) return;

        System.out.println("\n\n##### listener Cancel : " + canceled.toJson() + "\n\n");

        // Sample Logic //
        Approval approval = new Approval();
        approvalRepository.save(approval);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
