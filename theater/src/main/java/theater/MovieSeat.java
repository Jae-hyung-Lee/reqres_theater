package theater;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="MovieSeat_table")
public class MovieSeat {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String seatId;
    private Long movieId;
    private Long payId;
    private String status;

    @PrePersist
    public void onPrePersist(){
        SeatReserved seatReserved = new SeatReserved();
        BeanUtils.copyProperties(this, seatReserved);
        seatReserved.publishAfterCommit();


        SeatCanceled seatCanceled = new SeatCanceled();
        BeanUtils.copyProperties(this, seatCanceled);
        seatCanceled.publishAfterCommit();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }
    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }
    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
