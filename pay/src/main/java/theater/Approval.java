package theater;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Approval_table")
public class Approval {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Integer payId;
    private String bookId;
    private String customerId;

    @PrePersist
    public void onPrePersist(){
        Approved approved = new Approved();
        BeanUtils.copyProperties(this, approved);
        approved.publishAfterCommit();


        PaymentCanceled paymentCanceled = new PaymentCanceled();
        BeanUtils.copyProperties(this, paymentCanceled);
        paymentCanceled.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Integer getPayId() {
        return payId;
    }

    public void setPayId(Integer payId) {
        this.payId = payId;
    }
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }




}
