package theater;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Listinquery_table")
public class Listinquery {

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
