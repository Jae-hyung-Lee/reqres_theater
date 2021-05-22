# 영화예매 시스템

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
