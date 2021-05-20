package theater;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="MovieManagement_table")
public class MovieManagement {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long movieId;
    private String title;
    private String status;

    @PostPersist
    public void onPostPersist(){
        MovieRegistered movieRegistered = new MovieRegistered();
        BeanUtils.copyProperties(this, movieRegistered);
        movieRegistered.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
