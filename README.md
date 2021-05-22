# 2조 과제 : 영화예매 시스템
![image](https://user-images.githubusercontent.com/80744278/119227825-d0ccf880-bb4a-11eb-9f53-99faf04a0f35.png)

# 서비스 시나리오

기능적 요구사항
1. 고객이 영화를 선택하여 예매한다.
1. 고객이 결제한다.
1. 예매가 완료되면 예매 내역이 해당 극장에 전달된다.
1. 영화가 등록되면 상영관이 지정된다.
1. 해당 극장은 해당영화의 관람관 좌석를 예매처리 한다.
1. 고객은 예매를 취소할 수 있다.
1. 예매를 취소하면 좌석예매도 취소처리한다.
1. 고객은 예매현황을 조회할 수있다.
1. 예매 현황은 카톡으로 알려 준다.(예매 취소 포함)

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않으면 예매를 할 수 없다.(Sync호출)
1. 장애격리
    1. 예매관리 기능이 수행되지 않더라도 예매는 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 결제시스템이 과중되면 사용자를 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다  Circuit breaker, fallback
1. 성능
    1. 고객이 자주 예매관리에서 확인할 수 있는 예매현황을 예매시스템(프론트엔드)에서 확인할 수 있어야 한다  CQRS
    1. 예매상태가 바뀔때마다 카톡 등으로 알림을 줄 수 있어야 한다  Event driven


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

# 분석/설계
![image](https://user-images.githubusercontent.com/80908892/118935502-94509f80-b986-11eb-820d-7ad60bf637a0.png)

- movie서비스에서 cutomer조회 기능 제거
- app서비스에서 pay서비스로가는 cancel(pub/sub) 중복 표현 제거(3개 -> 1개)
![image](https://user-images.githubusercontent.com/80908892/118985614-8e27e680-b9b9-11eb-9b81-fe8d1196887e.png)

## 헥사고날 아키텍처 다이어그램 도출 (Polyglot)
![핵사고날_최종](https://user-images.githubusercontent.com/78134019/109744745-29f55200-7c16-11eb-8981-88924ad28cb3.jpg)

# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
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

## DDD 의 적용
총 5개의 Domain 으로 관리되고 있으며, 예약관리(Reservation) , 결재관리(Approval), 상영영화(MovieManagement), 영화좌석관리(MovieSeat), 영화관리(Movie)으로 구성하였습니다. 

```
package theater;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;

import org.springframework.beans.BeanUtils;

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
```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package theater;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="movieManagements", path="movieManagements")
public interface MovieManagementRepository extends PagingAndSortingRepository<MovieManagement, Long>{

}
```

- 적용 후 REST API 의 테스트
```
# Movie 등록
http POST http://localhost:8083/movieManagements movieId=10001 title="분노의 질주" status="opened"
http POST http://localhost:8083/movieManagements movieId=10002 title="미션 파서블" status="opened"
http POST http://localhost:8083/movieManagements movieId=10003 title="자산어보" status="opened"
http POST http://localhost:8083/movieManagements movieId=10004 title="간이역" status="opened"
http GET http://localhost:8083/movieManagements
http GET http://localhost:8084/movies
```

## 폴리글랏 퍼시스턴스

```
위치 : /reqres_theater>app>pom.xml
```
![image](https://user-images.githubusercontent.com/80744278/119217655-83369880-bb16-11eb-95f9-588fcfcebcbe.png)

## ★수정 필요★ 폴리글랏 프로그래밍 

영화 예매 서비스의 시나리오인 예약 완료/취소 상태 변경에 따라 고객에게 카톡메시지 보내는 기능의 구현 하였다. 구현체는 각 이벤트를 수신하여 처리하는 Kafka consumer 로 구현되었고 코드는 다음과 같다:
```
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

애플리케이션을 컴파일하고 실행하기 위한 도커파일은 아래와 같다
```
FROM python:2.7-slim
WORKDIR /app
ADD . /app
RUN pip install --trusted-host pypi.python.org -r requirements.txt
ENV NAME World
EXPOSE 8090
CMD ["python", "policy-handler.py"]
```

## 마이크로 서비스 호출 흐름

- 영화 예매 처리 

고객이 영화를 선택 후 예매(app) 후 결재요청(pay)은 (req/res) 되며,
결재 완료 후 해당 극장(theater) 좌석 선택 후 예매가 완료 됩니다.

고객이 영화를 선택 합니다. 
```
http POST http://localhost:8083/movieManagements movieId=10001 title="분노의 질주" status="opened"
http POST http://localhost:8083/movieManagements movieId=10002 title="미션 파서블" status="opened"
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


영화 선택 후 결재처리 합니다.   
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

결재가 완료 되면 해당 극장에서 영화와 관람관 좌석을 선택하여 예매를 완료 합니다. 
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

- 영화 예매 취소 처리

고객이 예약 현황(app)을 확인 하고 해당 예약을 취소하면 결재(pay) 및 
해당 극장(theater) 예약이 취소 됩니다. 

고객이 예약 취소를 요청 합니다.
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

예약 취소 요청를 완료하면 결재를 취소를 합니다. 
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

결재 취소가 되면 해당 극장 영화/좌석의 예약이 취소 됩니다.  
```
http localhost:808X/
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


- 고객 메시지 서비스 처리 

고객은 예약 완료, 예약 취소에 대한 메시지를 다음과 같이 받을 수 있으며, 관련 정보를 또한 확인 할 수 있습니다.

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


## Gateway 적용

서비스에 대한 하나의 접점을 만들기 위한 게이트웨이의 설정은 8088이며, 
예매관리,결재관리 및 극장정보등 마이크로서비스에 대한 일원화 된 접점을 제공하기 위한 설정 입니다.
```
app 서비스 : 8081
pay 서비스 : 8082
movie 서비스 : 8083
theater 서비스 : 8084
notice 서비스 : 8086
```

gateway > applitcation.yml 설정

![image](https://user-images.githubusercontent.com/80744278/119226171-c0188480-bb42-11eb-9039-dd83a40e9b9d.png)

- gateway 로컬 테스트

로컬 테스트는 다음과 같이 한글 서비스 호출로 테스트 되었습니다.

```
http localhost:8080/app
-> gateway 를 호출하나 8081 로 호출됨
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)


## 동기식 호출 과 Fallback 처리

영화 예약(app)후 결재처리(pay) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하였습니다.
REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 
```
 - 동기식 호출 영화 예약 후 결재 처리 관련 소스 추가
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

```
 - Fallback 처리 결과 
```
![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)



## 비동기식 호출 / 장애격리 / 성능

영화 예매 취소(app) 요청 이후 결재 취소(pay), 해당 극장 영화/좌석 취소(theater)는 비동기식 처리이므로, 
영화 예매 취소(app)의 서비스 호출에는 영향이 없도록 구성 합니다.

영화 예매 취소(app) 요청 후 결재 취소(pay)가 정상적으로 안되는 경우 
해당 극장 영화/좌석(theater)은 예약중으로 남게 됩니다.

<영화 예매 취소 요청>

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

<극장 영화/좌석 예약 상태>

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)

## 정보 조회 / View 조회
고객은 예약 현황을 조회 할 수 있습니다. 

![image](https://user-images.githubusercontent.com/80744278/119226000-015c6480-bb42-11eb-88df-b5a384ef2562.png)



## 소스 패키징

- 클라우드 배포를 위해서 다음과 같이 패키징 작업을 하였습니다.
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
cd notice
mvn clean && mvn package
cd ..
```
	
![image](https://user-images.githubusercontent.com/80744278/119227564-8a2ace80-bb49-11eb-88ce-e2df20a617a4.png)


※ 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW
