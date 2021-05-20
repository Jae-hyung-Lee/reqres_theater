package theater;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Detailinquery_table")
public class Detailinquery {

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
