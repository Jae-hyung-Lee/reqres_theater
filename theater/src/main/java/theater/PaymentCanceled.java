
package theater;

public class PaymentCanceled extends AbstractEvent {

    private Long id;
    private String payId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getPayId() {
        return payId;
    }

    public void setPayId(String payId) {
        this.payId = payId;
    }
}

