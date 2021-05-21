package theater;

import theater.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired ReservationRepository reservationRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApproved_Updatestate(@Payload Approved approved){

        if(!approved.validate()) return;

        System.out.println("\n\n##### listener Updatestate : " + approved.toJson() + "\n\n");

        // Sample Logic //
        Reservation reservation = new Reservation();
        reservationRepository.save(reservation);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCanceled_Updatestate(@Payload PaymentCanceled paymentCanceled){

        if(!paymentCanceled.validate()) return;

        System.out.println("\n\n##### listener Updatestate : " + paymentCanceled.toJson() + "\n\n");

        // Sample Logic //
        Reservation reservation = new Reservation();
        reservationRepository.save(reservation);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
