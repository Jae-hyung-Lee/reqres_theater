package theater;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Reservationavailabilityinquiry_table")
public class Reservationavailabilityinquiry {

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
