package theater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import theater.config.kafka.KafkaProcessor;

@Service
public class PolicyHandler {
    @Autowired
    MovieSeatRepository movieSeatRepository;
    @Autowired
    MovieRepository movieRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverApproved_Seatreserve(@Payload Approved approved) {

        if (!approved.validate())
            return;

        System.out.println("\n\n##### listener Seatreserve : " + approved.toJson() + "\n\n");

        /**
         * 좌석예약 등록
         */
        MovieSeat movieSeat = new MovieSeat();
        movieSeat.setPayId(approved.getPayId());
        movieSeat.setStatus("Reserved");
        movieSeatRepository.save(movieSeat);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCanceled_Seatcancel(@Payload PaymentCanceled paymentCanceled) {

        if (!paymentCanceled.validate())
            return;

        System.out.println("\n\n##### listener Seatcancel : " + paymentCanceled.toJson() + "\n\n");

        /**
         * 죄석예약 취소
         */
        MovieSeat movieSeat = new MovieSeat();
        movieSeat.setPayId(paymentCanceled.getPayId());
        movieSeat.setStatus("Reserved");
        movieSeatRepository.save(movieSeat);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMovieRegistered_RegisteMovie(@Payload MovieRegistered movieRegistered) {

        if (!movieRegistered.validate())
            return;

        System.out.println("\n\n##### listener RegisteMovie : " + movieRegistered.toJson() + "\n\n");

        /**
         * 영화등록
         */
        Movie movie = new Movie();
        movie.setMovieId(movieRegistered.getMovieId());
        movie.setTitle(movieRegistered.getTitle());
        movie.setStatus(movieRegistered.getStatus());
        movieRepository.save(movie);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString) {
        
    }

}
