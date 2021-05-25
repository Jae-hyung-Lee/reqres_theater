# 2조 과제 : 영화예매 시스템
![image](https://user-images.githubusercontent.com/80744278/119227825-d0ccf880-bb4a-11eb-9f53-99faf04a0f35.png)

---
# 서비스 시나리오

기능적 요구사항
1. 고객이 영화를 선택하여 예매한다.
1. 고객이 결제한다.
1. 예매가 완료되면 예매 내역이 해당 극장에 전달된다.
1. 영화가 등록되면 상영관이 지정된다.
1. 해당 극장은 해당영화의 관람관 좌석를 예매처리 한다.
1. 고객은 예매를 취소할 수 있다.
1. 예매를 취소하면 좌석예매도 취소처리한다.
1. 고객은 예매번호로 예매현황을 조회할 수있다.
1. 예매 현황은 카톡으로 알려 준다.(예매 취소 포함)

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않으면 예매를 할 수 없다.(Sync호출)
1. 장애격리
    1. 영화관리, 좌석관리 기능이 수행되지 않더라도 예매는 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다  Circuit breaker, fallback
1. 성능
    1. 고객이 자신의 예매번호를 이용하여 예매내역을 확인할 수 있어야 한다  CQRS
    1. 예매상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven

---
# 체크포인트

1. Saga
1. CQRS
1. Correlation
1. Req/Resp
1. Gateway
1. Deploy/ Pipeline
1. Circuit Breaker
1. Autoscale (HPA)
1. Zero-downtime deploy (Readiness Probe)
1. Config Map/ Persistence Volume
1. Polyglot
1. Self-healing (Liveness Probe)

---
# 분석/설계

### 이벤트 도출
![image](https://user-images.githubusercontent.com/80744278/119439561-98513880-bd5d-11eb-82b4-e90e886525a2.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/80744278/119439606-b028bc80-bd5d-11eb-8504-b76ca2b125e6.png)

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/80744278/119440657-b324ac80-bd5f-11eb-8f2f-bcd413e247fb.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/487999/79683618-52769680-8266-11ea-9c21-48d6812444ba.png)

    - app의 Order, store 의 주문처리, 결제의 결제이력은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌

### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/487999/79683625-560a1d80-8266-11ea-9790-40d68a36d95d.png)

## 이벤트스토밍
![image](https://user-images.githubusercontent.com/80908892/118935502-94509f80-b986-11eb-820d-7ad60bf637a0.png)

- movie서비스에서 cutomer조회 기능 제거
- app서비스에서 pay서비스로가는 cancel(pub/sub) 중복 표현 제거(3개 -> 1개)
![image](https://user-images.githubusercontent.com/80908892/118985614-8e27e680-b9b9-11eb-9b81-fe8d1196887e.png)

- movie서비스에서 deleteMovie 기능 제거 (현재 사용 이벤트 없음)
- MovieSeat 어그리게이션에서 불필요 항목 제거 (movieId, payId)
- MovieSeat, Movie 어그리게이션에 예매번호(bookId) 및 상영관번호(screenId) 컬럼 추가
- Approved 클래스에 필요 항목 추가 (bookId, movieId)
- User의 View 채널은 App으로 통일
- 기타 정비 및 현행화
![image](https://user-images.githubusercontent.com/81547613/119284217-1fc57b80-bc7a-11eb-8ad5-59d8efba8487.png)

* MSAEz 로 모델링한 이벤트스토밍 결과: http://www.msaez.io/#/storming/NsV4iwCaeqQOsNMyMgsTQ9Vaxgw2/share/c17526752f14346221c225bf4462d0df

---
## 헥사고날 아키텍처 다이어그램 도출 (Polyglot)
![image](https://user-images.githubusercontent.com/80744278/119308120-9f1d7400-bca7-11eb-8e66-d73cb66a6bda.png)

---
# 구현:

* 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다

```maven
cd app
mvn spring-boot:run

cd pay
mvn spring-boot:run 

cd movie
mvn spring-boot:run  

cd theater
mvn spring-boot:run

cd notice
mvn spring-boot:run
```

---
## DDD 의 적용
* 총 5개의 Domain 으로 관리되고 있으며, 예약관리(Reservation) , 결제관리(Approval), 상영영화관리(MovieManagement), 영화좌석관리(MovieSeat), 영화관리(Movie)으로 구성하였습니다. 

```java
package theater;

@Entity
@Table(name = "Reservation_table")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String bookId;
    private String customerId;
    private String movieId;
    private String payId;
    private String bookedYn;

    @PostLoad
    public void onPostLoad() {
        Logger logger = LoggerFactory.getLogger("Reservation");
        logger.info("Load");
    }

    @PrePersist
    public void onPrePersist() throws JsonProcessingException {
        Logger logger = LoggerFactory.getLogger("Reservation");
        logger.info("Make Reservation");

        Reserved reserved = new Reserved();
        BeanUtils.copyProperties(this, reserved);

        Approval approvalFromPay = AppApplication.applicationContext.getBean(theater.external.ApprovalService.class)
                .paymentRequest(this.bookId);

        if (approvalFromPay != null) {
            this.setPayId(approvalFromPay.getPayId());
            ObjectMapper objectMapper = new ObjectMapper();
            String approvalMessage = objectMapper.writeValueAsString(approvalFromPay);
        } else {
            logger.info("=======Pay didn't Approve. Confirm Pay Service.=======");
        }
    }

    @PreRemove
    public void onPostRemove() throws JsonProcessingException {
        Logger logger = LoggerFactory.getLogger("Reservation");
        logger.info("Make Canceled");

        Canceled canceled = new Canceled();
        BeanUtils.copyProperties(this, canceled);

        ObjectMapper objectMapper = new ObjectMapper();
        String canceledMessage = objectMapper.writeValueAsString(canceled);
        logger.info(canceledMessage);

        canceled.publishAfterCommit();

    }
}
```

* Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다


```java
package theater;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="reservations", path="reservations")
public interface ReservationRepository extends PagingAndSortingRepository<Reservation, Long>{

}
```

---
## 폴리글랏 퍼시스턴스

```
위치 : /reqres_theater>app>pom.xml
```
![image](https://user-images.githubusercontent.com/80744278/119217655-83369880-bb16-11eb-95f9-588fcfcebcbe.png)

## 폴리글랏 프로그래밍 

영화 예매 서비스의 시나리오인 예약 완료/취소 상태 변경에 따라 고객에게 카톡메시지 보내는 기능의 구현 하였다. 구현체는 각 이벤트를 수신하여 처리하는 Kafka consumer 로 구현되었고 코드는 다음과 같다:

```python
from flask import Flask
from redis import Redis, RedisError
from kafka import KafkaConsumer
import os
import socket


# To consume latest messages and auto-commit offsets
consumer = KafkaConsumer('fooddelivery',
                         group_id='',
                         bootstrap_servers=['localhost:9092'])
for message in consumer:
    print ("%s:%d:%d: key=%s value=%s" % (message.topic, message.partition,
                                          message.offset, message.key,
                                          message.value))

    # 카톡호출 API
```

* 애플리케이션을 컴파일하고 실행하기 위한 도커파일은 아래와 같다

```docker
FROM python:2.7-slim
WORKDIR /app
ADD . /app
RUN pip install --trusted-host pypi.python.org -r requirements.txt
ENV NAME World
EXPOSE 8090
CMD ["python", "policy-handler.py"]
```

---
## 마이크로 서비스 호출 흐름


### 영화 등록 처리 : 관리자가 영화를 등록 합니다.

* MOVIE 등록
```
http POST http://localhost:8083/movieManagements movieId="MOVIE-00001" title="어벤져스" status="RUNNING"
http POST http://localhost:8083/movieManagements movieId="MOVIE-00002" title="아이언맨" status="RUNNING"
http POST http://localhost:8083/movieManagements movieId="MOVIE-00003" title="토르" status="WAITING"
```

* Movie 서비스 내 MOVIE_MANAGEMENTS (영화관리) 테이블 데이터 생성 완료

```json
"movieManagements": [
    {
        "_links": {
            "movieManagement": {
                "href": "http://localhost:8083/movieManagements/1"
            },
            "self": {
                "href": "http://localhost:8083/movieManagements/1"
            }
        },
        "movieId": "MOVIE-00001",
        "status": "RUNNING",
        "title": "어벤져스"
    },
    {
        "_links": {
            "movieManagement": {
                "href": "http://localhost:8083/movieManagements/2"
            },
            "self": {
                "href": "http://localhost:8083/movieManagements/2"
            }
        },
        "movieId": "MOVIE-00002",
        "status": "RUNNING",
        "title": "아이언맨"
    },
    {
        "_links": {
            "movieManagement": {
                "href": "http://localhost:8083/movieManagements/3"
            },
            "self": {
                "href": "http://localhost:8083/movieManagements/3"
            }
        },
        "movieId": "MOVIE-00003",
        "status": "WAITING",
        "title": "토르"
    }
]
```

* Theater 서비스 내 MOVIES (좌석배정) 테이블 데이터 생성 완료

```json
"movies": [
    {
        "_links": {
            "movie": {
                "href": "http://localhost:8084/movies/1"
            },
            "self": {
                "href": "http://localhost:8084/movies/1"
            }
        },
        "movieId": "MOVIE-00001",
        "screenId": "어벤져스_상영관"
    },
    {
        "_links": {
            "movie": {
                "href": "http://localhost:8084/movies/2"
            },
            "self": {
                "href": "http://localhost:8084/movies/2"
            }
        },
        "movieId": "MOVIE-00002",
        "screenId": "아이언맨_상영관"
    },
    {
        "_links": {
            "movie": {
                "href": "http://localhost:8084/movies/3"
            },
            "self": {
                "href": "http://localhost:8084/movies/3"
            }
        },
        "movieId": "MOVIE-00003",
        "screenId": "토르_상영관"
    }
]
```



### 영화 예매 처리 : 고객이 영화와 좌석번호를 선택하여 예매를 요청하면 결재요청(pay)은 (req/res)되며, 결재 성공 시 극장(theater) 상영관 예매가 완료 됩니다.

* 예매 요청
```
http POST http://localhost:8081/reservations/new bookId="B1001" customerId="C1001" movieId="MOVIE-00001" seatId="A-1"
http POST http://localhost:8081/reservations/new bookId="B1002" customerId="C1002" movieId="MOVIE-00002" seatId="B-1"
http POST http://localhost:8081/reservations/new bookId="B1003" customerId="C1003" movieId="MOVIE-00003" seatId="C-1"
```

* PAY 서비스 내 APPROVALS (승인내역) 테이블 데이터 생성 완료

```json
"approvals": [
    {
        "_links": {
            "approval": {
                "href": "http://localhost:8082/approvals/1"
            },
            "self": {
                "href": "http://localhost:8082/approvals/1"
            }
        },
        "bookId": "B1001",
        "customerId": null,
        "movieId": "MOVIE-00001",
        "payId": "33bfd6df-6fbd-4d4d-b739-659c5199601e",
        "seatId": "A-1"
    },
    {
        "_links": {
            "approval": {
                "href": "http://localhost:8082/approvals/2"
            },
            "self": {
                "href": "http://localhost:8082/approvals/2"
            }
        },
        "bookId": "B1002",
        "customerId": null,
        "movieId": "MOVIE-00002",
        "payId": "d934f33b-b9f7-4c97-99ab-69a09e0c9612",
        "seatId": "B-1"
    },
    {
        "_links": {
            "approval": {
                "href": "http://localhost:8082/approvals/3"
            },
            "self": {
                "href": "http://localhost:8082/approvals/3"
            }
        },
        "bookId": "B1003",
        "customerId": null,
        "movieId": "MOVIE-00003",
        "payId": "bdcc11fd-12b7-4c4b-b09f-79ad5600f85c",
        "seatId": "C-1"
    }
]
```

* App 서비스 내 RESERVATIONS (예매내역) 테이블 데이터 생성 완료

```js
"reservations": [
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/1"
            },
            "self": {
                "href": "http://localhost:8081/reservations/1"
            }
        },
        "bookId": "B1001",
        "bookedYn": "Y",
        "customerId": "C1001",
        "movieId": "MOVIE-00001",
        "payId": "8044786d-c478-417e-84c4-11fe8e60f1f3",
        "seatId": "A-1"
    },
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/2"
            },
            "self": {
                "href": "http://localhost:8081/reservations/2"
            }
        },
        "bookId": "B1002",
        "bookedYn": "Y",
        "customerId": "C1002",
        "movieId": "MOVIE-00002",
        "payId": "3d95b581-55c6-47be-ba99-76e2239088fe",
        "seatId": "B-1"
    },
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/3"
            },
            "self": {
                "href": "http://localhost:8081/reservations/3"
            }
        },
        "bookId": "B1003",
        "bookedYn": "Y",
        "customerId": "C1003",
        "movieId": "MOVIE-00003",
        "payId": "57b41744-be82-469a-abb0-ed68ada367e1",
        "seatId": "C-1"
    }
]
```

* THEATER 서비스 내 MOVIE_SEATS (좌석배정내역) 테이블 데이터 생성 완료

```json
"movieSeats": [
    {
        "_links": {
            "movieSeat": {
                "href": "http://localhost:8084/movieSeats/6"
            },
            "self": {
                "href": "http://localhost:8084/movieSeats/6"
            }
        },
        "bookId": "B1001",
        "screenId": "어벤져스_상영관",
        "seatId": "A-1",
        "status": "Reserved"
    },
    {
        "_links": {
            "movieSeat": {
                "href": "http://localhost:8084/movieSeats/7"
            },
            "self": {
                "href": "http://localhost:8084/movieSeats/7"
            }
        },
        "bookId": "B1002",
        "screenId": "아이언맨_상영관",
        "seatId": "B-1",
        "status": "Reserved"
    },
    {
        "_links": {
            "movieSeat": {
                "href": "http://localhost:8084/movieSeats/8"
            },
            "self": {
                "href": "http://localhost:8084/movieSeats/8"
            }
        },
        "bookId": "B1003",
        "screenId": "토르_상영관",
        "seatId": "C-1",
        "status": "Reserved"
    }
]
```


### 영화 예매 취소 처리 : 고객이 특정 예약을 취소하면 결재(pay) 및 해당 극장(theater) 예약이 취소 됩니다.

* 예약 취소 요청
```
http DELETE http://localhost:8081/reservations/B1001    
http DELETE http://localhost:8081/reservations/B1002
http DELETE http://localhost:8081/reservations/B1003
```

* PAY 서비스 내 APPROVALS (승인내역) 테이블 데이터 삭제 완료
```
        "approvals": []
```

* APP 서비스 내 RESERVATIONS (예매내역) 테이블 STASUS 갱신 완료 

```json
"reservations": [
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/1"
            },
            "self": {
                "href": "http://localhost:8081/reservations/1"
            }
        },
        "bookId": "B1001",
        "bookedYn": "N",
        "customerId": "C1001",
        "movieId": "MOVIE-00001",
        "payId": "8044786d-c478-417e-84c4-11fe8e60f1f3",
        "seatId": "A-1"
    },
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/2"
            },
            "self": {
                "href": "http://localhost:8081/reservations/2"
            }
        },
        "bookId": "B1002",
        "bookedYn": "N",
        "customerId": "C1002",
        "movieId": "MOVIE-00002",
        "payId": "3d95b581-55c6-47be-ba99-76e2239088fe",
        "seatId": "B-1"
    },
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/3"
            },
            "self": {
                "href": "http://localhost:8081/reservations/3"
            }
        },
        "bookId": "B1003",
        "bookedYn": "N",
        "customerId": "C1003",
        "movieId": "MOVIE-00003",
        "payId": "57b41744-be82-469a-abb0-ed68ada367e1",
        "seatId": "C-1"
    }
]
```

* THEATER 서비스 내 MOVIE_SEATS (좌석배정내역) 테이블 STATUS 갱신 완료

```json
"reservations": [
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/1"
            },
            "self": {
                "href": "http://localhost:8081/reservations/1"
            }
        },
        "bookId": "B1001",
        "bookedYn": "N",
        "customerId": "C1001",
        "movieId": "MOVIE-00001",
        "payId": "8044786d-c478-417e-84c4-11fe8e60f1f3",
        "seatId": "A-1"
    },
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/2"
            },
            "self": {
                "href": "http://localhost:8081/reservations/2"
            }
        },
        "bookId": "B1002",
        "bookedYn": "N",
        "customerId": "C1002",
        "movieId": "MOVIE-00002",
        "payId": "3d95b581-55c6-47be-ba99-76e2239088fe",
        "seatId": "B-1"
    },
    {
        "_links": {
            "reservation": {
                "href": "http://localhost:8081/reservations/3"
            },
            "self": {
                "href": "http://localhost:8081/reservations/3"
            }
        },
        "bookId": "B1003",
        "bookedYn": "N",
        "customerId": "C1003",
        "movieId": "MOVIE-00003",
        "payId": "57b41744-be82-469a-abb0-ed68ada367e1",
        "seatId": "C-1"
    }
]
```

### 고객 메시지 서비스 처리 

* 고객은 예약 완료, 예약 취소에 대한 메시지를 다음과 같이 받을 수 있으며, 관련 정보를 또한 확인 할 수 있습니다.

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

---
## Gateway 적용

* 게이트웨이의 설정은 8080이며, 예매관리/결재관리 및 극장정보등 마이크로서비스에 대한 일원화 된 접점을 제공하기 위한 설정이다.
```
app 서비스 : 8081
pay 서비스 : 8082
movie 서비스 : 8083
theater 서비스 : 8084
notice 서비스 : 8086
```

* gateway > applitcation.yml 설정

![image](https://user-images.githubusercontent.com/81547613/119323918-2d9af100-bcba-11eb-8378-1338b6337b18.png)

### Gateway 테스트

* Gateway 테스트는 다음과 같이 확인하였다.

> kubectl get all
![image](https://user-images.githubusercontent.com/81547613/119357273-5505b400-bce2-11eb-854d-12930219b3ad.png)

> External IP:8080 을 통한 API호출을 통해 http://app:8080 으로부터 응답을 받음
![image](https://user-images.githubusercontent.com/81547613/119356977-fdffdf00-bce1-11eb-8dc6-b098b5ef0975.png)

---
## 동기식 호출 과 Fallback 처리

영화 예약(app)후 결재처리(pay) 간의 호출은 동기식 일관성을 유지하기 위하여 FeignClient 를 이용하여 호출하도록 한다. 

### 동기식 호출 영화 예약 후 결재 처리
#### Local 환경에서 확인

* App Service는 실행 상태 / Pay Service는 중단 상태
```http
http POST http://localhost:8081/reservations/new bookId="B1001" customerId="C1001" movieId="MOVIE-00001" seatId="A-1"
```

* Response Message : Pay disapproved가 뜨면서 Pay 서비스를 확인하라는 문구를 보낸다.
```
>http POST http://localhost:8081/reservations/new bookId="B1001" customerId="C1001" movieId="MOVIE-00001" seatId="A-1"
HTTP/1.1 400
Connection: close
Content-Type: application/json;charset=UTF-8
Date: Mon, 24 May 2021 05:30:15 GMT
Transfer-Encoding: chunked

{
    "details": "uri=/reservations/new",
    "message": "[B1001] Pay disapproved, You must confirm Pay Service",
    "timestamp": "2021-05-24T05:30:15.807+0000"
}
```

```
- Fallback 처리 결과 
- Spring Boot Log Message
2021-05-24 14:02:11.708 DEBUG 25811 --- [strix-pay-api-1] theater.external.ApprovalService         : [ApprovalService#paymentRequest] ---> GET http://localhost:8082/approved?bookId=B1001&movieId=MOVIE-00001&seatId=A-1 HTTP/1.1
2021-05-24 14:02:11.714 DEBUG 25811 --- [strix-pay-api-1] theater.external.ApprovalService         : [ApprovalService#paymentRequest] <--- ERROR ConnectException: Connection refused (Connection refused) (5ms)
2021-05-24 14:02:11.719  INFO 25811 --- [strix-pay-api-1] t.f.ApprovalServiceFallbackFactory       : ========= FallbackFactory called: Confirm Pay Service =========
2021-05-24 14:02:11.719  INFO 25811 --- [strix-pay-api-1] t.f.ApprovalServiceFallbackFactory       : Error Message: Connection refused (Connection refused) executing GET http://localhost:8082/approved?bookId=B1001&movieId=MOVIE-00001&seatId=A-1
2021-05-24 14:02:11.721  INFO 25811 --- [nio-8081-exec-1] theater.ReservationController            : ========= Pay didn't Approve. Confirm Pay Service.=========
```

#### 쿠버네티스에서 확인

---
## 비동기식 호출 / 장애격리 / 성능

영화 예매 취소(app) 요청과 결재 취소(pay), 해당 극장 영화/좌석 취소(theater)는 비동기식 처리이므로, 다른 시스템의 상태가 영화 예매 취소(app)의 서비스 호출에 영향이 없도록 구성한다.
즉, 결재 취소 시스템이 비정상일 경우 영화/좌석은 취소되지 않고, 결재 시스템이 재기동 되면 이벤트를 구독하여 취소시킨다.

* 영화 예매 취소 전 영화/좌석 예약 상태

```json
{
    "_links": {
        "movieSeat": {
            "href": "http://localhost:8084/movieSeats/9"
        },
        "self": {
            "href": "http://localhost:8084/movieSeats/9"
        }
    },
    "bookId": "B1004",
    "screenId": "어벤져스_상영관",
    "seatId": "D-1",
    "status": "Reserved"
}
```

* PAY 서비스 다운
```
2021-05-24 15:04:04.998  INFO 8732 --- [       Thread-8] o.s.i.monitor.IntegrationMBeanExporter   : Summary on shutdown: _org.springframework.integration.errorLogger.handler
2021-05-24 15:04:05.000  INFO 8732 --- [       Thread-8] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown initiated...
2021-05-24 15:04:05.003  INFO 8732 --- [       Thread-8] com.zaxxer.hikari.HikariDataSource       : HikariPool-1 - Shutdown completed.
[INFO] ----일괄 작업을 끝내시겠습니까 (Y/N)? y

C:\Dev-Workspace\reqres_theater\pay>
```

* 영화 예매 취소 요청
```
>http DELETE http://localhost:8081/reservations/B1004
HTTP/1.1 204
Date: Mon, 24 May 2021 06:05:26 GMT
```

* 극장 영화/좌석 예약 상태 확인 : 예약취소되지 않음 (Reserved)

```json
{
    "_links": {
        "movieSeat": {
            "href": "http://localhost:8084/movieSeats/9"
        },
        "self": {
            "href": "http://localhost:8084/movieSeats/9"
        }
    },
    "bookId": "B1004",
    "screenId": "어벤져스_상영관",
    "seatId": "D-1",
    "status": "Reserved"
}
```
---
## 정보 조회 / View 조회
고객은 예약 현황을 조회 할 수 있습니다. 

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


---
#운영
---
## 소스 패키징

### 클라우드 배포를 위한 패키징 작업
```
cd app
mvn clean && mvn package
cd ..
cd pay
mvn clean && mvn package
cd ..
cd movie
mvn clean && mvn package
cd ..
cd theater
mvn clean && mvn package
cd ..
cd gateway
mvn clean && mvn package
cd ..
```
	
![image](https://user-images.githubusercontent.com/80744278/119227564-8a2ace80-bb49-11eb-88ce-e2df20a617a4.png)

---
## 클라우드 배포/운영 파이프라인

### aws 클라우드에 배포하기 위한 주요 정보 설정

```
클러스터 명 : user02-eks
```

### aws config - IAM => 엑세스관리 > 사용자
```json
$ aws configure
AWS Access Key ID [None]: AKIAQYU2ROSK4JEJH2VM
AWS Secret Access Key [None]: ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Default region name [None]: ap-northeast-2
Default output format [None]: json

#설정확인
$cat ~/.aws/config
[default]
region = ap-northeast-2
output = json

#인증정보 확인
$cat ~/.aws/credentials
[default]
aws_access_key_id = AKIAQYU2ROSK4JEJH2VM
aws_secret_access_key = ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
```

### EKS생성 (클러스터생성 및 설정)
```json
$eksctl create cluster --name skccteam02-eks --version 1.17 --nodegroup-name standard-workers --node-type t3.medium --nodes 4 --nodes-min 1 --nodes-max 4
$kubectl get nodes

$aws eks --region ap-northeast-2 update-kubeconfig --name team02-eks
$kubectl config current-context
$kubectl get all
```

### 네임스페이스 생성

```shell
kubectl create ns team02
kubectl create ns kafka
kubectl get ns
```
![image](https://user-images.githubusercontent.com/80908892/119248843-d2d89b00-bbce-11eb-8359-6d6af7ba5fb1.png)

### 기본 namespace설정 및 서비스확인
```sh
kubectl config set-context $(kubectl config current-context) --namespace=team02
kubectl get all
```

### helm(3.X설치)

> helm 설치 스크립트를 다운 받음

```shll
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
```

```shell
root@labs-1227910482:/home/project/team# 
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 11248  100 11248    0     0  33981      0 --:--:-- --:--:-- --:--:-- 33981
```

> 다운 받은 스크립트 실행

```shell
chmod 700 get_helm.sh
./get_helm.sh
```

```shell
Downloading https://get.helm.sh/helm-v3.5.4-linux-amd64.tar.gz
Verifying checksum... Done.
Preparing to install helm into /usr/local/bin
helm installed into /usr/local/bin/helm
```

> repository 추가

```shell
helm repo add incubator https://charts.helm.sh/incubator
```

```shell
WARNING: Kubernetes configuration file is group-readable. This is insecure. Location: /root/.kube/config
WARNING: Kubernetes configuration file is world-readable. This is insecure. Location: /root/.kube/config
"incubator" has been added to your repositories
root@labs-1227910482:/home/project/team# helm repo update
WARNING: Kubernetes configuration file is group-readable. This is insecure. Location: /root/.kube/config
WARNING: Kubernetes configuration file is world-readable. This is insecure. Location: /root/.kube/config
Hang tight while we grab the latest from your chart repositories...
...Successfully got an update from the "incubator" chart repository
Update Complete. ⎈Happy Helming!⎈
```

### kafka설치
```shell
helm install my-kafka --namespace kafka incubator/kafka
$ kubectl get all -n kafka
```
![image](https://user-images.githubusercontent.com/81547613/119323673-ee6ca000-bcb9-11eb-8564-863fe6384430.png)

### 도커 이미지 만들고 레지스트리에 등록하기

1. Docker를 이용해 이미지 Build
2. AWS에 Container Registry 생성
3. 생성된 ECR에 이미지를 Push

```shell
cd app
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-app:v1 .
aws ecr create-repository --repository-name user02-app --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-app:v1
cd ..

cd movie
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-movie:v1 .
aws ecr create-repository --repository-name user02-movie --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-movie:v1
cd ..

cd pay
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-pay:v1 .
aws ecr create-repository --repository-name user02-pay --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-pay:v1
cd ..

cd theater
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-theater:v1 .
aws ecr create-repository --repository-name user02-theater --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-theater:v1
cd ..

cd gateway
docker build -t 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-gateway:v1 .
aws ecr create-repository --repository-name user02-gateway --region ap-northeast-2
docker push 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-gateway:v1
cd ..
```


### deployment.yml로 서비스 배포

### 각 마이크로 서비스를 yml 파일을 사용하여 배포 합니다.
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app
  labels:
    app: app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: app
  template:
    metadata:
      labels:
        app: app
    spec:
      containers:
        - name: app
          image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-app:v1
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

```
#### 배포처리
```shell
cd app
kubectl apply -f ./kubernetes --namespace=team02
cd ..

cd pay
kubectl apply -f ./kubernetes --namespace=team02
cd ..

cd movie
kubectl apply -f ./kubernetes --namespace=team02
cd ..

cd theater
kubectl apply -f ./kubernetes --namespace=team02
cd ..

cd gateway
kubectl create deploy gateway --image=052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-gateway:v1
kubectl expose deploy gateway --type="LoadBalancer" --port=8080
```

![image](https://user-images.githubusercontent.com/80908892/119253234-3c66a280-bbeb-11eb-9904-11bdedeebd69.png)

---
## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현하였습니다.

### Hystrix 를 설정:  

요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정


```yaml
# application.yml
feign:
  pay-api:
    url: http://localhost:8082
  httpclient:
    connection-timeout: 1
  hystrix:
    enabled: true
  client:
    config:
      default:
        loggerLevel: BASIC
hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

### 부하테스트


* Siege 리소스 생성

```
kubectl run siege --image=apexacme/siege-nginx -n team03
```

* 실행

```
kubectl exec -it pod/siege-5459b87f86-hlfm9 -c siege -n team03 -- /bin/bash
```

*부하 실행

```bash
siege -c200 -t60S -r10 -v --content-type "application/json" http://app:8080/isolations
```

```java
public class ReservationController {
    @GetMapping("/isolations")
    String isolation() {
        return approvalService.isolation();
    }
}

```
```java
public class ApprovalServiceFallbackFactory implements FallbackFactory<ApprovalService> {

    @Override
    public ApprovalService create(Throwable cause) {
        return new ApprovalService() {

            @Override
            public String isolation() {
                return "fallback";
            }
        };
    }
}
```

- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 pay 서비스에서 처리되면서 
다시 pay에서 서비스를 받기 시작 합니다

![image](https://user-images.githubusercontent.com/80908892/119437873-f3812c00-bd59-11eb-93e2-760ff7784a0d.png)

- report

![image](https://user-images.githubusercontent.com/80908892/119437975-20cdda00-bd5a-11eb-86d5-36fec86da983.png)

---
## 오토스케일 아웃

### deployment.yml 설정

```yaml
spec:
  containers:
    - name: movie
      image: 052937454741.dkr.ecr.ap-northeast-2.amazonaws.com/user02-movie:v1
      ports:
        - containerPort: 8080
      readinessProbe:
        httpGet:
          path: '/actuator/health'
          port: 8080
        initialDelaySeconds: 10
        timeoutSeconds: 2
        periodSeconds: 5
        failureThreshold: 10
      livenessProbe:
        httpGet:
          path: '/actuator/health'
          port: 8080
        initialDelaySeconds: 120
        timeoutSeconds: 2
        periodSeconds: 5
        failureThreshold: 5
      resources:
        limits:
          cpu: "500m"
        requests:
          cpu: "200m"
```

### Auto Scale 설정

```shell
kubectl autoscale deployment movie --cpu-percent=50 --min=1 --max=10
```

### 부하기능 API 추가

```java
@RestController
public class MovieManagementController {


    @GetMapping("/hpa")
    public String getHPA() {
        Random rng = new Random();
        long loopCnt = 0;

        while (loopCnt < 100) {
            double r = rng.nextFloat();
            double v = Math.sin(Math.cos(Math.sin(Math.cos(r))));
            System.out.println(String.format("r: %f, v %f", r, v));
            loopCnt++;
        }

        return "hpa";
    }
}
```

### 부하기능 API 수행

> 명령어

```shell
kubectl exec -it pod/stresstool-85fb8c78db-77fb4 -- /bin/bash
siege -c60 -t60S -v http://movie:8080/hpa
```

> 결과

```shell
HTTP/1.1 200     0.04 secs:       3 bytes ==> GET  /hpa
HTTP/1.1 200     0.04 secs:       3 bytes ==> GET  /hpa
HTTP/1.1 200     0.04 secs:       3 bytes ==> GET  /hpa
HTTP/1.1 200     0.03 secs:       3 bytes ==> GET  /hpa
HTTP/1.1 200     0.01 secs:       3 bytes ==> GET  /hpa
HTTP/1.1 200     0.14 secs:       3 bytes ==> GET  /hpa
HTTP/1.1 200     0.06 secs:       3 bytes ==> GET  /hpa

Lifting the server siege...
Transactions:                   8314 hits
Availability:                 100.00 %
Elapsed time:                  59.19 secs
Data transferred:               0.02 MB
Response time:                  0.42 secs
Transaction rate:             140.46 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                   59.42
Successful transactions:        8314
Failed transactions:               0
Longest transaction:            5.30
Shortest transaction:           0.00
```

### 오토스케일링에 대한 모니터링:
![image](https://user-images.githubusercontent.com/80908892/119321129-45bd4100-bcb7-11eb-81b3-bd83a29251da.png)


---
## 무정지 재배포

### 현재 POD 개수 확인 (2개)

```shell
NAME                         READY   STATUS    RESTARTS   AGE
pod/movie-5f6c5757cb-5q2j8   1/1     Running   0          7m43s
pod/movie-5f6c5757cb-m7brh   1/1     Running   0          5m13s
pod/multitool                1/1     Running   1          10h
pod/order-666f44f458-rj6tt   1/1     Running   1          10h
pod/stresstool               1/1     Running   0          28m
```

### deployment.yml에 readiness 옵션을 추가 

```shell
- deployment.yml
#####################################################
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
#####################################################
```

### 부하 기능 API 추가
```java
- MovieManagementController.java
#####################################################
@GetMapping("/serviceAddress")
public String getServiceAddress () {
    String serviceAddress = null;
    if (serviceAddress == null) {
        serviceAddress = findMyHostname() + "/" + findMyIpAddress();
    }
    return serviceAddress;
}

private String findMyHostname() {
    try {
        return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
        return "unknown host name";
    }
}

private String findMyIpAddress() {
    try {
        return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
        return "unknown IP address";
    }
}
#####################################################
```

### 부하 기능 api 수행

```shell
# multitool Container 내부로 들어감.
kubectl exec -it pod/multitool -- /bin/sh
while true; do curl movie:8080/serviceAddress; echo echo ""; sleep 1; done
http http://movie:8080/actuator/health
http put http://movie:8080/actuator/down 
```


### Readiness 테스트 수행결과

* 정상적으로 요청처리 가능한 POD의 응답 수신

```shell
movie-5f6c5757cb-5q2j8/10.1.1.211echo  <<<< 최초 2개의 POD에 대하여 번갈아가며 수행가능상태 응답
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo  <<<< 한개의 POD 중지요청 (Health down)
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-5q2j8/10.1.1.211echo
movie-5f6c5757cb-m7brh/10.1.1.212echo  <<<< 한개의 POD만 응답 (요청처리 가능상태)
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
movie-5f6c5757cb-m7brh/10.1.1.212echo
```

---
## Config Map

### apllication.yml 설정

```yaml
pay:
  mid: ${MID}
```

### Deployment.yml 설정

```yaml
env:
- name: MID
  valueFrom:
    configMapKeyRef:
      name: mid-cm
      key: mid
```

### config map 생성 후 조회

> Config Map 생성

```shell
kubectl create configmap mid-cm --from-literal=mid=pg0000123
configmap/mid-cm created
```

> Config Map 확인

```shell
kubectl get cm
NAME     DATA   AGE
mid-cm   1      42s
```

- 설정한 url로 주문 호출
  
```http
http 20.194.36.201:8080/taxicalls tel="01012345678" status="call" location="mapo" cost=25000
```

![configmap5](https://user-images.githubusercontent.com/31096538/109798744-7c5a6100-7c5e-11eb-8aaa-03fa8277cee6.JPG)

- configmap 삭제 후 app 서비스 재시작

```shell
# Config Map 삭제
kubectl delete configmap apiurl -n team03
# App Service 재시작
kubectl get pod/taxicall-74f7dbc967-mtbmq -n team03 -o yaml | kubectl replace --force -f-
```

![configmap6](https://user-images.githubusercontent.com/31096538/109798766-811f1500-7c5e-11eb-8008-1b9073cb6722.JPG)

- configmap 삭제된 상태에서 주문 호출 

```http
http 20.194.36.201:8080/taxicalls tel="01012345678" status="call" location="mapo" cost=25000
kubectl get all -n team03
```

![configmap7](https://user-images.githubusercontent.com/31096538/109798785-85e3c900-7c5e-11eb-8769-ab416b1e17b2.JPG)


![configmap8](https://user-images.githubusercontent.com/31096538/109798805-8bd9aa00-7c5e-11eb-8d05-1db2457d3611.JPG)


![configmap9](https://user-images.githubusercontent.com/31096538/109798824-9005c780-7c5e-11eb-9d5b-6f14f9b6bba9.JPG)

---

## Persistance Volume

### PVC 관련 YAML 생성

```shell
ll

total 24
drwxr-xr-x  2 root root 6144 May 25 02:12 ./
drwxr-xr-x 10 root root 6144 May 25 01:59 ../
-rw-r--r--  1 root root  872 May 25 02:12 efs-provisioner-deploy.yaml
-rw-r--r--  1 root root  126 May 25 01:59 efs-provisioner-storageclass.yaml
-rw-r--r--  1 root root 1330 May 25 01:59 rbac.yaml
-rw-r--r--  1 root root   89 May 25 01:59 service_account.yaml
```

```shell
cat efs-provisioner-deploy.yaml 
```

```yaml
kind: Deployment
apiVersion: apps/v1
metadata:
  namespace: team02
  name: efs-provisioner
spec:
  selector:
    matchLabels:
      app: efs-provisioner
  replicas: 1
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: efs-provisioner
    spec:
      serviceAccount: efs-provisioner
      containers:
        - name: efs-provisioner
          image: quay.io/external_storage/efs-provisioner:v2.4.0
          env:
            - name: FILE_SYSTEM_ID
              value: fs-1d2e9d7d
            - name: AWS_REGION
              value: ap-northeast-2
            - name: PROVISIONER_NAME
              value: my-aws.com/aws-efs
          volumeMounts:
            - name: pvcs
              mountPath: /pvcs
      volumes:
        - name: pvcs
          nfs:
            server: fs-1d2e9d7d.efs.ap-northeast-2.amazonaws.com
            path: /
```

### YAML 파일 적용 및 SC, PVC 상태확인

```shell
kubectl apply -f .
```

```shell
serviceaccount/efs-provisioner created
clusterrole.rbac.authorization.k8s.io/efs-provisioner-runner created
clusterrolebinding.rbac.authorization.k8s.io/run-efs-provisioner created
role.rbac.authorization.k8s.io/leader-locking-efs-provisioner created
rolebinding.rbac.authorization.k8s.io/leader-locking-efs-provisioner created
deployment.apps/efs-provisioner configured
storageclass.storage.k8s.io/efs-provisioner configured
```

```shell
kubectl get sc
```

```shell
NAME              PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
aws-efs           my-aws.com/aws-efs      Delete          Immediate              false                  10h
efs-provisioner   user02-efs              Delete          Immediate              false                  11h
gp2 (default)     kubernetes.io/aws-ebs   Delete          WaitForFirstConsumer   false                  21h
```

> 서비스 확인

```shell
kubectl get pvc
```

```
NAME      STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
aws-efs   Bound    pvc-0f90e1d5-8edd-491d-bcfe-4c8f86a606b3   1Mi        RWX            aws-efs        10h
```

### PV 적용한 POD(mypod) 내 텍스트파일 생성
```
root@labs-1227910482:/home/project/reqres_theater/movie/kubernetes# kubectl exec -it pod/mypod -- bin/bash
```
```
root@mypod:/mnt/aws# echo "team02 message.!!!" >> msg.txt
echo "team02 message.vi!" >> msg.txt
root@mypod:/mnt/aws# ls
msg.txt
```
```
root@mypod:/mnt/aws# cat msg.txt 
team02 message.vi!
```

### MOVIE POD 내 텍스트파일 생성여부 확인

```shell
kubectl exec -it pod/movie-67d777cc7d-78j9r -- bin/sh
```

```shell
/ # ls
app.jar  dev      home     media    opt      root     sbin     sys      usr
bin      etc      lib      mnt      proc     run      srv      tmp      var
/ # cd mnt
/mnt # ls
aws
/mnt # cd aws
```

```shell
/mnt/aws # ls
msg.txt
/mnt/aws # cat msg.txt 
team02 message.vi!
```

---
## Self-healing (Liveness Probe)


### deployment.yml 에 Liveness Probe 옵션 추가


```yaml
livenessProbe:
    httpGet:
        path: '/actuator/health'
        port: 8080
    initialDelaySeconds: 120
    timeoutSeconds: 2
    periodSeconds: 5
    failureThreshold: 5
```

* Movie 시스템의 health를 down 시키면 READY가 0/1로 내려갔다가 RESTART가 1로 올라가며 다시 시스템이 살아나는 것을 확인함

![image](https://user-images.githubusercontent.com/80908892/119315005-53bb9380-bcb0-11eb-868f-46da0ae46814.png)

---
※ 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW
