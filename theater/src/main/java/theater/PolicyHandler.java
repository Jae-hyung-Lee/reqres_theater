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
    @Autowired MovieSeatRepository movieSeatRepository;
    @Autowired MovieRepository movieRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApproved_Seatreserve(@Payload Approved approved){

        if(!approved.validate()) return;

        System.out.println("\n\n##### listener Seatreserve : " + approved.toJson() + "\n\n");

        // Sample Logic //
        MovieSeat movieSeat = new MovieSeat();
        movieSeatRepository.save(movieSeat);
        Movie movie = new Movie();
        movieRepository.save(movie);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCanceled_Seatcancel(@Payload PaymentCanceled paymentCanceled){

        if(!paymentCanceled.validate()) return;

        System.out.println("\n\n##### listener Seatcancel : " + paymentCanceled.toJson() + "\n\n");

        // Sample Logic //
        MovieSeat movieSeat = new MovieSeat();
        movieSeatRepository.save(movieSeat);
        Movie movie = new Movie();
        movieRepository.save(movie);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMovieRegistered_RegisteMovie(@Payload MovieRegistered movieRegistered){

        if(!movieRegistered.validate()) return;

        System.out.println("\n\n##### listener RegisteMovie : " + movieRegistered.toJson() + "\n\n");

        // Sample Logic //
        MovieSeat movieSeat = new MovieSeat();
        movieSeatRepository.save(movieSeat);
        Movie movie = new Movie();
        movieRepository.save(movie);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
