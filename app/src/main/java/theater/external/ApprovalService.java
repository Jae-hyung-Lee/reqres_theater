
package theater.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="pay", url="http://pay:8080")
public interface ApprovalService {

    @RequestMapping(method= RequestMethod.GET, path="/approvals")
    public void paymentrequest(@RequestBody Approval approval);

}