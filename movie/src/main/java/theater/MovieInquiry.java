package theater;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="MovieInquiry_table")
public class MovieInquiry {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

}
