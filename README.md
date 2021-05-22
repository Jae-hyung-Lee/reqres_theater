# 2조 과제 : 영화예매 시스템

- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW

# 서비스 시나리오

기능적 요구사항
1. 고객이 영화를 선택하여 예매한다.
1. 고객이 결제한다.
1. 예매가 완료되면 예매 내역이 해당 극장에 전달된다.
1. 영화가 등록되면 상영관이 반드시 지정된다.
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

## ★수정 필요★ 마이크로 서비스 호출 흐름 - 시나리오 확인 및 내용 추가 

- 영화 예매 처리 

고객이 영화를 선택 후 예매(app) 후 결재요청(pay)은 (req/res) 되며,
결재 완료 후 영화 예매 상태가 완료 됩니다.

로컬에서는 다음과 같이 영화 선택 한다. 
```
http POST http://localhost:8083/movieManagements movieId=10001 title="분노의 질주" status="opened"
http POST http://localhost:8083/movieManagements movieId=10002 title="미션 파서블" status="opened"
```
![taxicall1](https://user-images.githubusercontent.com/78134019/109771576-51611480-7c40-11eb-8754-94d35a5703ec.png)

![taxicall2](https://user-images.githubusercontent.com/78134019/109771589-545c0500-7c40-11eb-997a-90249ea8f912.png)


영화 선택 후 결재처리 합니다.   
```
http POST http://localhost:8083/movieManagements movieId=10001 title="분노의 질주" status="opened"
http POST http://localhost:8083/movieManagements movieId=10002 title="미션 파서블" status="opened"
```

![1](https://user-images.githubusercontent.com/7607807/109840083-16380300-7c8b-11eb-80d1-5eb6815ac53a.png)


결재가 완료 되면 해당 극장에서 영화와 관람관 좌석을 선택하여 예매를 완료 합니다. 

![3](https://user-images.githubusercontent.com/78134019/109771602-58882280-7c40-11eb-93c4-a3831156c151.png)

![4](https://user-images.githubusercontent.com/78134019/109771654-69d12f00-7c40-11eb-9d2c-4807f0c3d726.png)

![5](https://user-images.githubusercontent.com/78134019/109771661-6c338900-7c40-11eb-8a4a-9a758a8d1613.png)

클라우드 상에서도 마찬가지 입니다.

![3](https://user-images.githubusercontent.com/7607807/109840275-50090980-7c8b-11eb-9f37-0ca07115308e.png)

![1](https://user-images.githubusercontent.com/7607807/109841386-564bb580-7c8c-11eb-8262-bf28c1a2bd70.png)
- taxicall 서비스 호출 취소 처리

호출 취소는 택시호출에서 다음과 같이 호출 하나를 취소 함으로써 진행 합니다.

```
http delete http://localhost:8081/택시호출s/1
HTTP/1.1 204
Date: Tue, 02 Mar 2021 16:59:12 GMT
```

클라우드 상에서 호출 취소는 다음과 같습니다.
```
http delete http://20.194.36.201:8080/taxicalls/1
HTTP/1.1 204
Date: Tue, 02 Mar 2021 16:59:12 GMT
```


호출이 취소 되면 아래와 같이 택시 호출이 하나가 삭제 되었고, 

```
http localhost:8081/택시호출s/
http 20.194.36.201:8080/taxicalls
```

![6](https://user-images.githubusercontent.com/78134019/109771698-7a81a500-7c40-11eb-964e-a07e989f997c.png)

![1](https://user-images.githubusercontent.com/7607807/109840796-cb6abb00-7c8b-11eb-8cb9-0d623fe11043.png)

택시관리에서는 해당 호출의 호출 상태가 호출취소로 상태가 변경 됩니다.

```
http localhost:8082/택시관리s/
http 20.194.36.201:8080/taximanags
```

![7](https://user-images.githubusercontent.com/78134019/109771726-83727680-7c40-11eb-88bd-169a8d6184fe.png)

![2](https://user-images.githubusercontent.com/7607807/109840982-f3f2b500-7c8b-11eb-9373-00844726bb04.png)

- 고객 메시지 서비스 처리 

고객(customer)는 호출 확정과 할당 확정에 대한 메시지를 다음과 같이 받을 수 있으며,
할당 된 택시기사의 정보를 또한 확인 할 수 있습니다.
파이썬으로 구현 하였습니다.

![8](https://user-images.githubusercontent.com/78134019/109771811-9ab16400-7c40-11eb-8a49-57156a4d0c8e.png)


## Gateway 적용

서비스에 대한 하나의 접점을 만들기 위한 게이트웨이의 설정은 8088이며, 
택시호출,택시관리 및 택시할당 마이크로서비스에 대한 일원화 된 접점을 제공하기 위한 설정 입니다.
```
택시호출 서비스 : 8081
택시관리 서비스 : 8082
택시할당 서비스 : 8083
```

gateway > applitcation.yml 설정

![gateway_1](https://user-images.githubusercontent.com/78134019/109480363-c73d7280-7abe-11eb-9904-0c18e79072eb.png)

아래 설정은 DDD를 통해서 구현 된 한글화 된 서비스를 클라우드에서 호출 할 경우, 문제가 발생하여 모든 도메인 명 및 서비스를 
영어로 재 구현 하였으며, 이에 대한 게이트웨이 처리를 보여주고 있습니다.

![gateway_2](https://user-images.githubusercontent.com/78134019/109480386-d02e4400-7abe-11eb-9251-a813ac911e0d.png)


- gateway 로컬 테스트

로컬 테스트는 다음과 같이 한글 서비스 호출로 테스트 되었습니다.

```
http localhost:8080/택시호출s
-> gateway 를 호출하나 8081 로 호출됨
```
![gateway_3](https://user-images.githubusercontent.com/78134019/109480424-da504280-7abe-11eb-988e-2a6d7a1f7cea.png)




# Table of contents

- [예제 - 영화예매](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

[주제선정]
- 
# 서비스 시나리오

기능적 요구사항
1. 고객이 영화를 선택하여 예매한다.
1. 고객이 결제한다.
1. 예매가 완료되면 예매 내역이 해당 극장에 전달된다.
1. 영화가 등록되면 상영관이 반드시 지정된다.
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
- 평가 항목
  - Saga
  - CQRS
  - Correlation
  - Req/Resp
  - Gateway
  - Deploy/ Pipeline
  - Circuit Breaker
  - Autoscale (HPA)
  - Zero-downtime deploy (Readiness Probe)
  - Config Map/ Persistence Volume
  - Polyglot
  - Self-healing (Liveness Probe)

- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?
