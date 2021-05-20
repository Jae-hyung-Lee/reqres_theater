package theater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import theater.config.kafka.KafkaProcessor;

@Service
public class MovieInquiryViewHandler {


    @Autowired
    private MovieInquiryRepository movieInquiryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenMovieRegistered_then_CREATE_1 (@Payload MovieRegistered movieRegistered) {
        try {

            if (!movieRegistered.validate()) return;

            // view 객체 생성
            MovieInquiry movieInquiry = new MovieInquiry();
            // view 객체에 이벤트의 Value 를 set 함
            // movieInquiry.setTitle(movieRegistered.get());
            // movieInquiry.setMovieId(movieRegistered.get());
            // view 레파지 토리에 save
            movieInquiryRepository.save(movieInquiry);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}