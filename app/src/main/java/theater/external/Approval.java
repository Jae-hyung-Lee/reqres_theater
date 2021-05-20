package theater.external;

public class Approval {

    private Long id;
    private Integer payId;
    private String bookId;
    private String customerId;

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
